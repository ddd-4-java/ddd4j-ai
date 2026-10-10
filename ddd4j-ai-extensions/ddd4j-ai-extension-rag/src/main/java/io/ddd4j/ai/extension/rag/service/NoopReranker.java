package io.ddd4j.ai.extension.rag.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 默认直通重排实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class NoopReranker implements Reranker {

    /**
     * 构造 NoopReranker 对象。
     */
    public NoopReranker() {
    }

    /**
     * 直通返回候选列表，不改变顺序（默认零开销策略）。
     *
     * @param query      查询语句（本实现忽略）
     * @param candidates 候选文档列表
     * @return 原样返回的候选列表
     */
    @Override
    public List<Document> rerank(String query, List<Document> candidates) {
        return candidates;
    }
}
