package io.ddd4j.ai.extension.rag.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 默认直通重排实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class NoopReranker implements Reranker {

    @Override
    public List<Document> rerank(String query, List<Document> candidates) {
        return candidates;
    }
}
