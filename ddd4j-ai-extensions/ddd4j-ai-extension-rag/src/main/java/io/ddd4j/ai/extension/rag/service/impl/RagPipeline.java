package io.ddd4j.ai.extension.rag.service.impl;

import io.ddd4j.ai.extension.chat.service.ChatService;
import io.ddd4j.ai.extension.rag.properties.RagProperties;
import io.ddd4j.ai.extension.rag.service.RagService;
import io.ddd4j.ai.extension.rag.service.Reranker;
import io.ddd4j.ai.extension.vectordb.service.VectorDbService;
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

    /**
     * 构造 RAG 管道：四要素均非 null。
     *
     * @param chatService     对话端口
     * @param vectorDbService 向量库端口
     * @param reranker        重排器
     * @param properties      RAG 配置
     * @throws NullPointerException 任一要素为 null 时
     */
    public RagPipeline(ChatService chatService, VectorDbService vectorDbService,
                       Reranker reranker, RagProperties properties) {
        this.chatService = Objects.requireNonNull(chatService, "chatService");
        this.vectorDbService = Objects.requireNonNull(vectorDbService, "vectorDbService");
        this.reranker = Objects.requireNonNull(reranker, "reranker");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    /**
     * 摄取文档：按配置决定是否 Token 分块，随后写入向量库。
     *
     * @param documents 待摄取文档列表
     * @throws NullPointerException documents 为 null 时
     */
    @Override
    public void ingest(List<Document> documents) {
        Objects.requireNonNull(documents, "documents");
        List<Document> chunks = properties.isChunking()
                ? new TokenTextSplitter().apply(documents)
                : documents;
        vectorDbService.add(chunks);
    }

    /**
     * 同步问答：检索 → 重排 → 模板增强 → 单次生成。
     *
     * @param question 用户问题
     * @return 生成的回答
     */
    @Override
    public String query(String question) {
        return chatService.chat(augment(question));
    }

    /**
     * 流式问答：检索 → 重排 → 模板增强 → 流式生成。
     *
     * @param question 用户问题
     * @return 回答增量流
     */
    @Override
    public Flux<String> queryStream(String question) {
        return chatService.streamChat(augment(question));
    }

    /**
     * 增强查询：检索 topK 候选、重排后拼接为 {information}，渲染 prompt 模板。
     *
     * @param question 用户问题
     * @return 渲染后的增强 prompt
     */
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
