package io.ddd4j.ai.extension.rag.service.impl;

import io.ddd4j.ai.extension.chat.service.ChatService;
import io.ddd4j.ai.extension.rag.properties.RagProperties;
import io.ddd4j.ai.extension.rag.service.Reranker;
import io.ddd4j.ai.extension.vectordb.service.VectorDbService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RagPipeline} 单元测试：mock chat/vectordb 端口与 Reranker，
 * 验证摄取分块、检索→重排→增强生成链路。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class RagPipelineTest {

    private final ChatService chat = mock(ChatService.class);
    private final VectorDbService vectorDb = mock(VectorDbService.class);
    private final RagProperties properties = new RagProperties();

    private RagPipeline pipeline(Reranker reranker) {
        return new RagPipeline(chat, vectorDb, reranker, properties);
    }

    @Test
    void ingestAddsDocumentsToVectorDb() {
        String longText = "Ddd4j AI 是 Ddd4j Boot 生态的 AI 能力扩展项目。".repeat(40);
        List<Document> docs = List.of(new Document(longText, Map.of()));

        pipeline(Reranker.NOOP).ingest(docs);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(vectorDb).add(captor.capture());
        assertThat(captor.getValue()).isNotEmpty();   // 长文本被分块为多片
    }

    @Test
    void ingestWithoutChunkingKeepsOriginalDocuments() {
        properties.setChunking(false);
        Document doc = new Document("短内容", Map.of());

        pipeline(Reranker.NOOP).ingest(List.of(doc));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(vectorDb).add(captor.capture());
        assertThat(captor.getValue()).containsExactly(doc);
    }

    @Test
    void querySearchesReranksThenGenerates() {
        Document hit1 = new Document("Ddd4j AI 是 AI 扩展项目", Map.of());
        Document hit2 = new Document("无关内容", Map.of());
        when(vectorDb.search(eq("什么是 Ddd4j AI"), anyInt(), isNull()))
                .thenReturn(new java.util.ArrayList<>(List.of(hit1, hit2)));
        // 只保留第一个片段的重排器
        Reranker keepFirst = (query, candidates) -> List.of(candidates.get(0));
        when(chat.chat(any(String.class))).thenReturn("回答");

        String answer = pipeline(keepFirst).query("什么是 Ddd4j AI");

        assertThat(answer).isEqualTo("回答");
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chat).chat(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("Ddd4j AI 是 AI 扩展项目")
                .doesNotContain("无关内容")
                .contains("什么是 Ddd4j AI");
    }

    @Test
    void queryStreamUsesAugmentedPrompt() {
        when(vectorDb.search(any(String.class), anyInt(), any()))
                .thenReturn(List.of(new Document("片段", Map.of())));
        when(chat.streamChat(any(String.class))).thenReturn(Flux.just("流式回答"));

        List<String> chunks = pipeline(Reranker.NOOP)
                .queryStream("问题").collectList().block();

        assertThat(chunks).containsExactly("流式回答");
        verify(chat).streamChat(org.mockito.ArgumentMatchers.argThat(
                (String prompt) -> prompt.contains("片段") && prompt.contains("问题")));
    }
}
