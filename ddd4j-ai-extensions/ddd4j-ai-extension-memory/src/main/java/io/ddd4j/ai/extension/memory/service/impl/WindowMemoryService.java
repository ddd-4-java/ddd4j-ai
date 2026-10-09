package io.ddd4j.ai.extension.memory.service.impl;

import io.ddd4j.ai.extension.memory.service.MemoryService;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 窗口记忆实现：委托 Spring AI {@link MessageWindowChatMemory}，
 * 存储后端由构造传入的 {@link ChatMemoryRepository} 决定
 * （内存 / 官方 JDBC / Redis repository 等均可），窗口大小可配。
 *
 * @author ddd4j
 */
public class WindowMemoryService implements MemoryService {

    private final MessageWindowChatMemory delegate;

    /**
     * 创建窗口记忆服务。
     *
     * @param repository 存储后端，不允许 {@code null}
     * @param windowSize 窗口大小（每个会话保留的最近消息条数）
     */
    public WindowMemoryService(ChatMemoryRepository repository, int windowSize) {
        this.delegate = MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(windowSize)
                .build();
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        delegate.add(conversationId, messages);
    }

    @Override
    public List<Message> get(String conversationId) {
        return delegate.get(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        delegate.clear(conversationId);
    }
}
