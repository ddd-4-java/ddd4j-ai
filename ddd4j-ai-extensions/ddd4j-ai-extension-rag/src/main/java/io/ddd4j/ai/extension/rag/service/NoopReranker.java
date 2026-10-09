package io.ddd4j.ai.extension.rag.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 默认直通重排实现。
 *
 * @author ddd4j
 */
public class NoopReranker implements Reranker {

    /**
     * 默认构造器（直通实现无状态）。
     */
    public NoopReranker() {
    }

    @Override
    public List<Document> rerank(String query, List<Document> candidates) {
        return candidates;
    }
}
