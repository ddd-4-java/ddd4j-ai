package io.ddd4j.ai.cmpt.memory.service.impl;

import io.ddd4j.ai.cmpt.memory.service.MemoryService;
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
