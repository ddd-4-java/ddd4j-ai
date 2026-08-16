package io.ddd4j.ai.cmpt.memory.service;

import org.springframework.ai.chat.memory.ChatMemory;

/**
 * 会话记忆端口：以 conversationId 维度存取对话消息。
 *
 * <p>有意继承 Spring AI 的 {@link ChatMemory} 抽象（而非平行自建端口）：
 * 一方面复用其成熟的 add/get/clear 语义，另一方面使本端口可直接喂给
 * {@code MessageChatMemoryAdvisor} 等 Spring AI 生态组件。存储后端与窗口策略由实现决定。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface MemoryService extends ChatMemory {

    /**
     * 默认窗口大小（保留最近 N 条消息）。
     */
    int DEFAULT_WINDOW_SIZE = 20;
}
