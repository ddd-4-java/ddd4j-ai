package io.ddd4j.ai.samples.rag;

import io.ddd4j.ai.extension.rag.service.RagService;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * rag 检索增强组件使用示例：知识入库与基于知识库的问答。
 *
 * <p>前提：chat（模型 starter）与 vectordb（向量库 starter）端口就绪。
 *
 * @author ddd4j
 */
@Component
public class RagSample {

    private final RagService ragService;

    /**
     * 构造 RagSample。
     *
     * @param ragService RAG 检索服务
     */
    public RagSample(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * 摄取文本知识（自动分块入库）。
     *
     * @param knowledgeId 知识库标识
     * @param content 知识文本
     */
    public void ingest(String knowledgeId, String content) {
        ragService.ingest(List.of(new Document(content, Map.of("knowledgeId", knowledgeId))));
    }

    /**
     * 基于知识库问答（检索 → 重排 → 增强生成）。
     *
     * @param question 用户提问内容
     * @return 回答文本
     */
    public String ask(String question) {
        return ragService.query(question);
    }

    /**
     * 基于知识库流式问答。
     *
     * @param question 用户提问内容
     * @return 响应式数据流
     */
    public reactor.core.publisher.Flux<String> askStream(String question) {
        return ragService.queryStream(question);
    }
}
