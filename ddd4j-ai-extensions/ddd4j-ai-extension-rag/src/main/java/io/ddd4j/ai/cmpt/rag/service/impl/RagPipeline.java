package io.ddd4j.ai.cmpt.rag.service.impl;

import io.ddd4j.ai.cmpt.chat.service.ChatService;
import io.ddd4j.ai.cmpt.rag.properties.RagProperties;
import io.ddd4j.ai.cmpt.rag.service.RagService;
import io.ddd4j.ai.cmpt.rag.service.Reranker;
import io.ddd4j.ai.cmpt.vectordb.service.VectorDbService;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * RAG 管道实现：组合 chat / vectordb 端口与可插拔 {@link Reranker}。
 * 摄取 = 分块（可配）→ 入库；查询 = 检索 → 重排 → 模板增强 → 生成。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class RagPipeline implements RagService {

    private final ChatService chatService;

    private final VectorDbService vectorDbService;

    private final Reranker reranker;

    private final RagProperties properties;

    public RagPipeline(ChatService chatService, VectorDbService vectorDbService,
                       Reranker reranker, RagProperties properties) {
        this.chatService = Objects.requireNonNull(chatService, "chatService");
        this.vectorDbService = Objects.requireNonNull(vectorDbService, "vectorDbService");
        this.reranker = Objects.requireNonNull(reranker, "reranker");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public void ingest(List<Document> documents) {
        Objects.requireNonNull(documents, "documents");
        List<Document> chunks = properties.isChunking()
                ? new TokenTextSplitter().apply(documents)
                : documents;
        vectorDbService.add(chunks);
    }

    @Override
    public String query(String question) {
        return chatService.chat(augment(question));
    }

    @Override
    public Flux<String> queryStream(String question) {
        return chatService.streamChat(augment(question));
    }

    private String augment(String question) {
        List<Document> candidates = vectorDbService.search(question, properties.getTopK(), null);
        List<Document> reranked = reranker.rerank(question, candidates);

        String information = reranked.stream()
                .map(Document::getText)
                .collect(Collectors.joining(System.lineSeparator()));

        return new PromptTemplate(properties.getPromptTemplate())
                .render(Map.of("information", information, "question", question));
    }
}
