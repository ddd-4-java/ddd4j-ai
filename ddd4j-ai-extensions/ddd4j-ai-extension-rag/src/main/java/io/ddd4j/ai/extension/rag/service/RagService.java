package io.ddd4j.ai.extension.rag.service;

import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 检索增强生成端口：摄取（文档入库）与查询（检索 → 重排 → 增强生成）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface RagService {

    /**
     * 摄取文档：按配置分块后写入向量库。
     *
     * @param documents 原始文档
     */
    void ingest(List<Document> documents);

    /**
     * 增强查询：检索相关片段 → 重排 → 拼装增强 prompt → 生成回答。
     *
     * @param question 用户问题
     * @return 基于知识库的回答
     */
    String query(String question);

    /**
     * 增强查询（流式）。
     *
     * @param question 用户问题
     * @return 回答内容流
     */
    Flux<String> queryStream(String question);
}
