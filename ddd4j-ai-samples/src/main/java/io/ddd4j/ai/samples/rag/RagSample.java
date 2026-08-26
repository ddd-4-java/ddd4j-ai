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
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Component
public class RagSample {

    private final RagService ragService;

    public RagSample(RagService ragService) {
        this.ragService = ragService;
    }

    /** 摄取文本知识（自动分块入库）。 */
    public void ingest(String knowledgeId, String content) {
        ragService.ingest(List.of(new Document(content, Map.of("knowledgeId", knowledgeId))));
    }

    /** 基于知识库问答（检索 → 重排 → 增强生成）。 */
    public String ask(String question) {
        return ragService.query(question);
    }

    /** 基于知识库流式问答。 */
    public reactor.core.publisher.Flux<String> askStream(String question) {
        return ragService.queryStream(question);
    }
}
