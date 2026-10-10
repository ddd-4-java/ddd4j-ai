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
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class WindowMemoryService implements MemoryService {

    private final MessageWindowChatMemory delegate;

    /**
     * 构造窗口记忆服务。
     *
     * @param repository 会话存储后端（内存 / JDBC / Redis 等）
     * @param windowSize 每会话保留的最近消息条数
     */
    public WindowMemoryService(ChatMemoryRepository repository, int windowSize) {
        this.delegate = MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(windowSize)
                .build();
    }

    /**
     * 追加会话消息（超出窗口时按策略淘汰最旧消息）。
     *
     * @param conversationId 会话标识
     * @param messages       待追加消息列表
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        delegate.add(conversationId, messages);
    }

    /**
     * 读取会话全部保留消息。
     *
     * @param conversationId 会话标识
     * @return 消息列表（无记录时为空列表）
     */
    @Override
    public List<Message> get(String conversationId) {
        return delegate.get(conversationId);
    }

    /**
     * 清空指定会话的全部消息。
     *
     * @param conversationId 会话标识
     */
    @Override
    public void clear(String conversationId) {
        delegate.clear(conversationId);
    }
}
