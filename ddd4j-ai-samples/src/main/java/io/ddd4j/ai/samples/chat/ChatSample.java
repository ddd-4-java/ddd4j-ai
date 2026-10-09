package io.ddd4j.ai.samples.chat;

import io.ddd4j.ai.extension.chat.service.ChatService;
import org.springframework.stereotype.Component;

/**
 * chat 对话组件使用示例：单轮 / 多轮（会话记忆）/ 流式调用。
 *
 * <p>前提：业务服务引入模型 starter（如 {@code spring-ai-starter-model-openai} 或
 * {@code spring-ai-starter-model-ollama}）提供 ChatModel，再引入
 * {@code ddd4j-ai-extension-chat}（多轮需 {@code ddd4j-ai-extension-memory}）。
 *
 * @author ddd4j
 */
@Component
public class ChatSample {

    private final ChatService chatService;

    /**
     * 构造 ChatSample。
     *
     * @param chatService 对话服务
     */
    public ChatSample(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 单轮问答。
     *
     * @param question 用户提问内容
     * @return 回答文本
     */
    public String ask(String question) {
        return chatService.chat(question);
    }

    /**
     * 多轮问答：同一 conversationId 内自动携带历史上下文。
     *
     * @param question 用户提问内容
     * @param conversationId 会话标识
     * @return 文本结果
     */
    public String askInSession(String question, String conversationId) {
        return chatService.chat(question, conversationId);
    }

    /**
     * 流式问答：增量消费模型输出（如 SSE 推送）。
     *
     * @param question 用户提问内容
     * @return 响应式数据流
     */
    public reactor.core.publisher.Flux<String> askStream(String question) {
        return chatService.streamChat(question);
    }
}
