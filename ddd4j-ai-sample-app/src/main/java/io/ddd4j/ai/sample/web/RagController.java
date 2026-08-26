package io.ddd4j.ai.sample.web;

import io.ddd4j.ai.extension.rag.service.RagService;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * RAG 演示端点：知识摄取 + 基于知识库问答。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@RestController
@RequestMapping("/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /** 摄取知识：POST /rag/ingest?content=... */
    @PostMapping("/ingest")
    public Map<String, String> ingest(@RequestParam("content") String content) {
        ragService.ingest(List.of(new Document(content, Map.of("source", "sample-app"))));
        return Map.of("status", "ingested");
    }

    /** 知识库问答：GET /rag/ask?q=... */
    @GetMapping("/ask")
    public Map<String, String> ask(@RequestParam("q") String question) {
        return Map.of("answer", ragService.query(question));
    }
}
