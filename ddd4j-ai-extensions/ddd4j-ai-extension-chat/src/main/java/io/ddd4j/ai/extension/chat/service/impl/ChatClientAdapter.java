package io.ddd4j.ai.extension.chat.service.impl;

import io.ddd4j.ai.extension.chat.service.ChatService;
import io.ddd4j.ai.extension.memory.service.MemoryService;
import io.ddd4j.ai.core.AiHandler;
import io.ddd4j.ai.core.AiRequest;
import io.ddd4j.ai.core.AiResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 对话适配器：包装 Spring AI {@link ChatClient}，同时作为 core 的 {@link AiHandler} 暴露。
 * 多轮对话通过组合 {@link MemoryService}（读取历史 → 请求 → 写回本轮问答）实现，
 * 不依赖 ChatClient 的 advisor 机制，保持端口可独立替换。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class ChatClientAdapter implements ChatService, AiHandler {

    public static final String NAME = "ddd4j-ai-chat";

    private final ChatClient chatClient;

    private final MemoryService memoryService;

    private final String defaultSystemPrompt;

    public ChatClientAdapter(ChatClient chatClient, MemoryService memoryService, String defaultSystemPrompt) {
        this.chatClient = Objects.requireNonNull(chatClient, "chatClient");
        this.memoryService = memoryService;
        this.defaultSystemPrompt = defaultSystemPrompt;
    }

    @Override
    public String chat(String message) {
        return requestSpec(message).call().content();
    }

    @Override
    public String chat(String message, String conversationId) {
        Objects.requireNonNull(conversationId, "conversationId");
        MemoryService memory = requireMemory();

        UserMessage userMessage = new UserMessage(message);
        List<Message> messages = new ArrayList<>(memory.get(conversationId));
        if (defaultSystemPrompt != null && !defaultSystemPrompt.isBlank()) {
            messages.add(new org.springframework.ai.chat.messages.SystemMessage(defaultSystemPrompt));
        }
        messages.add(userMessage);

        String answer = chatClient.prompt(new Prompt(messages)).call().content();

        memory.add(conversationId, List.of(userMessage, new AssistantMessage(answer)));
        return answer;
    }

    @Override
    public Flux<String> streamChat(String message) {
        return requestSpec(message).stream().content();
    }

    @Override
    public AiResponse handle(AiRequest request) {
        return AiResponse.of(chat(request.input()));
    }

    @Override
    public String name() {
        return NAME;
    }

    private ChatClient.ChatClientRequestSpec requestSpec(String message) {
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt().user(message);
        if (defaultSystemPrompt != null && !defaultSystemPrompt.isBlank()) {
            spec = spec.system(defaultSystemPrompt);
        }
        return spec;
    }

    private MemoryService requireMemory() {
        if (memoryService == null) {
            throw new IllegalStateException(
                    "多轮对话需要 MemoryService：请引入 ddd4j-ai-extension-memory 组件");
        }
        return memoryService;
    }
}
