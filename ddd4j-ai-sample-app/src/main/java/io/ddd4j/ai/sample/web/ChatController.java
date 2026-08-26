package io.ddd4j.ai.sample.web;

import io.ddd4j.ai.extension.chat.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 对话演示端点：单轮 / 多轮（会话记忆）/ 流式（SSE）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** 单轮：GET /chat?q=你好 */
    @GetMapping
    public Map<String, String> chat(@RequestParam("q") String question) {
        return Map.of("answer", chatService.chat(question));
    }

    /** 多轮：同一 conversationId 内自动携带历史。POST /chat/session */
    @PostMapping("/session")
    public Map<String, String> chatInSession(@RequestParam("q") String question,
                                             @RequestParam(value = "conversationId", defaultValue = "demo") String conversationId) {
        return Map.of("conversationId", conversationId, "answer",
                chatService.chat(question, conversationId));
    }

    /** 流式 SSE：GET /chat/stream */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam("q") String question) {
        return chatService.streamChat(question);
    }
}
