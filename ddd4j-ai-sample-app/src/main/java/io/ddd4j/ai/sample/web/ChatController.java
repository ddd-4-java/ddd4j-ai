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
 * @author ddd4j
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    /**
     * 构造对话演示控制器。
     *
     * @param chatService 对话服务
     */
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 单轮对话：GET /chat?q=你好。
     *
     * @param question 用户提问内容
     * @return 包含回答的键值对
     */
    @GetMapping
    public Map<String, String> chat(@RequestParam("q") String question) {
        return Map.of("answer", chatService.chat(question));
    }

    /**
     * 多轮对话：同一 conversationId 内自动携带历史。POST /chat/session。
     *
     * @param question       用户提问内容
     * @param conversationId 会话标识，默认 {@code demo}
     * @return 包含会话标识与回答的键值对
     */
    @PostMapping("/session")
    public Map<String, String> chatInSession(@RequestParam("q") String question,
                                             @RequestParam(value = "conversationId", defaultValue = "demo") String conversationId) {
        return Map.of("conversationId", conversationId, "answer",
                chatService.chat(question, conversationId));
    }

    /**
     * 流式对话（SSE）：GET /chat/stream。
     *
     * @param question 用户提问内容
     * @return 逐段返回回答内容的响应流
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam("q") String question) {
        return chatService.streamChat(question);
    }
}
