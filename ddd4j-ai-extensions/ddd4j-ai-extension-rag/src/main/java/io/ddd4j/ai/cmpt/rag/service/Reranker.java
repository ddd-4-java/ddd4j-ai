package io.ddd4j.ai.cmpt.rag.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 检索结果重排扩展点：对初筛片段做二次排序/过滤（如按相关度模型、业务规则）。
 * 默认实现为直通（{@link NoopReranker}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface Reranker {

    /**
     * 对候选片段重排。
     *
     * @param query      用户问题
     * @param candidates 初筛片段
     * @return 重排后的片段
     */
    List<Document> rerank(String query, List<Document> candidates);

    /**
     * 直通重排：原样返回候选。
     */
    NoopReranker NOOP = new NoopReranker();
}
