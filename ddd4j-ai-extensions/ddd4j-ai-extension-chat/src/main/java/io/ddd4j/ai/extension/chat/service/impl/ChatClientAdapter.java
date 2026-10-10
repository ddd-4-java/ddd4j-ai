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

    /** 适配器在 {@link io.ddd4j.ai.core.AiHandler} 体系中的注册名。 */
    public static final String NAME = "ddd4j-ai-chat";

    private final ChatClient chatClient;

    private final MemoryService memoryService;

    private final String defaultSystemPrompt;

    /**
     * 构造对话适配器。
     *
     * @param chatClient         Spring AI 对话客户端（非空）
     * @param memoryService      记忆服务；为 {@code null} 时多轮入口会抛出异常
     * @param defaultSystemPrompt 默认系统提示词；为空时不注入 system 消息
     * @throws NullPointerException 当 {@code chatClient} 为 {@code null} 时
     */
    public ChatClientAdapter(ChatClient chatClient, MemoryService memoryService, String defaultSystemPrompt) {
        this.chatClient = Objects.requireNonNull(chatClient, "chatClient");
        this.memoryService = memoryService;
        this.defaultSystemPrompt = defaultSystemPrompt;
    }

    /**
     * 单轮同步对话：用户消息直接经 Spring AI 客户端调用。
     *
     * @param message 用户消息
     * @return 模型回答文本
     */
    @Override
    public String chat(String message) {
        return requestSpec(message).call().content();
    }

    /**
     * 多轮同步对话：读取会话历史 + 默认系统提示词 → 调用模型 → 把本轮问答写回记忆。
     *
     * @param message        用户消息
     * @param conversationId 会话标识（非空）
     * @return 模型回答文本
     * @throws NullPointerException 当 {@code conversationId} 为 {@code null} 时
     * @throws IllegalStateException 当未装配 {@link MemoryService} 时
     */
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

    /**
     * 单轮流式对话：以 Flux 逐段产出模型回答。
     *
     * @param message 用户消息
     * @return 模型回答的增量文本流
     */
    @Override
    public Flux<String> streamChat(String message) {
        return requestSpec(message).stream().content();
    }

    /**
     * core {@link AiHandler} 门面：把请求输入交给单轮对话并包装为 {@link AiResponse}。
     *
     * @param request AI 请求
     * @return 装载模型回答的 AI 响应
     */
    @Override
    public AiResponse handle(AiRequest request) {
        return AiResponse.of(chat(request.input()));
    }

    /**
     * 返回适配器注册名。
     *
     * @return {@link #NAME}
     */
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
