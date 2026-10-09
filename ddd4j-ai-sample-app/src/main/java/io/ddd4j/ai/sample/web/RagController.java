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
 * @author ddd4j
 */
@RestController
@RequestMapping("/rag")
public class RagController {

    private final RagService ragService;

    /**
     * 构造 RAG 演示控制器。
     *
     * @param ragService RAG 服务
     */
    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * 摄取知识：POST /rag/ingest?content=...。
     *
     * @param content 待摄取的知识文本
     * @return 摄取结果状态
     */
    @PostMapping("/ingest")
    public Map<String, String> ingest(@RequestParam("content") String content) {
        ragService.ingest(List.of(new Document(content, Map.of("source", "sample-app"))));
        return Map.of("status", "ingested");
    }

    /**
     * 知识库问答：GET /rag/ask?q=...。
     *
     * @param question 用户提问内容
     * @return 基于知识库的回答
     */
    @GetMapping("/ask")
    public Map<String, String> ask(@RequestParam("q") String question) {
        return Map.of("answer", ragService.query(question));
    }
}
