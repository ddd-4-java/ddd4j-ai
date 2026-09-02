package io.ddd4j.ai.samples.integrated.ragpipeline;

import io.ddd4j.ai.extension.chat.service.ChatService;
import io.ddd4j.ai.extension.document.DocumentReader;
import io.ddd4j.ai.extension.embedding.service.EmbeddingService;
import io.ddd4j.ai.extension.rag.service.RagService;
import io.ddd4j.ai.extension.vectordb.service.VectorDbService;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 场景 2：RAG 全链路（文档→OCR→embedding→vectordb→rag→chat）。
 * <p>
 * 组件串联：{@code document（解析）→ embedding（向量化）→ vectordb（存储）→ rag（检索+生成）→ chat（兜底）}
 */
@Component
public class RagPipelineSample {

    private final DocumentReader documentReader;
    private final EmbeddingService embeddingService;
    private final VectorDbService vectorDbService;
    private final RagService ragService;
    private final ChatService chatService;

    public RagPipelineSample(DocumentReader documentReader, EmbeddingService embeddingService,
                             VectorDbService vectorDbService, RagService ragService, ChatService chatService) {
        this.documentReader = documentReader;
        this.embeddingService = embeddingService;
        this.vectorDbService = vectorDbService;
        this.ragService = ragService;
        this.chatService = chatService;
    }

    /**
     * 文档摄取：解析文件 → 提取文本 → 向量化 → 存入向量库 → 注入 RAG 知识库。
     */
    public void ingestDocument(File file) throws Exception {
        var doc = documentReader.read(file);
        ragService.ingest(List.of(new Document(doc.fullMarkdown(),
                Map.of("source", file.getName()))));
    }

    /**
     * RAG 问答：检索知识库 → 上下文注入 → LLM 生成回答。
     */
    public String askWithRag(String question) {
        return ragService.query(question);
    }

    /**
     * RAG 流式问答：逐 token 返回（适合 SSE）。
     */
    public Flux<String> askWithRagStream(String question) {
        return ragService.queryStream(question);
    }

    /**
     * 直接检索（不含 LLM 生成）：返回最相关的文档片段。
     */
    public List<Document> searchKnowledge(String query) {
        return vectorDbService.search(query, 5, null);
    }
}
