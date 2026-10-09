package io.ddd4j.ai.samples.memory;

import io.ddd4j.ai.extension.memory.service.MemoryService;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * memory 会话记忆组件使用示例：会话消息的写入、读取与清空。
 * 默认进程内存储；引入官方 chat-memory-repository-jdbc/redis starter 后自动切换持久化后端。
 *
 * @author ddd4j
 */
@Component
public class MemorySample {

    private final MemoryService memoryService;

    /**
     * 构造 MemorySample。
     *
     * @param memoryService 会话记忆服务
     */
    public MemorySample(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * 记录一轮问答。
     *
     * @param conversationId 会话标识
     * @param question 用户提问内容
     * @param answer 回答内容
     */
    public void record(String conversationId, String question, String answer) {
        memoryService.add(conversationId, List.of(
                new UserMessage(question),
                new AssistantMessage(answer)));
    }

    /**
     * 读取会话历史（窗口策略自动截断）。
     *
     * @param conversationId 会话标识
     * @return 结果列表
     */
    public List<Message> history(String conversationId) {
        return memoryService.get(conversationId);
    }

    /**
     * 清空指定会话。
     *
     * @param conversationId 会话标识
     */
    public void forget(String conversationId) {
        memoryService.clear(conversationId);
    }
}
