package io.ddd4j.ai.samples.integrated.chatmemsse;

import io.ddd4j.ai.extension.chat.service.ChatService;
import io.ddd4j.ai.extension.memory.service.MemoryService;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 场景 1：多轮记忆对话 + 流式输出。
 * <p>
 * 组件串联：{@code chat（对话）+ memory（会话记忆）}
 * <p>
 * 展示：同一 conversationId 内自动携带历史，支持 SSE 流式推送。
 */
@Component
public class ChatMemorySseSample {

    private final ChatService chatService;
    private final MemoryService memoryService;

    public ChatMemorySseSample(ChatService chatService, MemoryService memoryService) {
        this.chatService = chatService;
        this.memoryService = memoryService;
    }

    /**
     * 多轮对话：同一 conversationId 自动写入/读取记忆历史。
     */
    public String multiTurn(String conversationId, String userMessage) {
        // memoryService 被 ChatClientAdapter 自动集成，无需手动管理
        return chatService.chat(userMessage, conversationId);
    }

    /**
     * 流式对话：返回 Flux 供 SSE 逐 token 推送。
     */
    public Flux<String> streamFlux(String userMessage) {
        return chatService.streamChat(userMessage);
    }

    /**
     * 手动管理记忆：读取历史 → 拼接上下文 → 手动调用（脱离 ChatService 自动集成时使用）。
     */
    public String manualMemoryChat(String conversationId, String userMessage) {
        List<Message> history = memoryService.get(conversationId);
        StringBuilder context = new StringBuilder();
        for (Message msg : history) {
            context.append(msg.getText()).append("\n");
        }
        context.append("用户：").append(userMessage);
        String answer = chatService.chat(context.toString());
        memoryService.add(conversationId, List.of(
                new UserMessage(userMessage),
                new AssistantMessage(answer)));
        return answer;
    }

    /**
     * 清除会话记忆。
     */
    public void clearMemory(String conversationId) {
        memoryService.clear(conversationId);
    }
}
