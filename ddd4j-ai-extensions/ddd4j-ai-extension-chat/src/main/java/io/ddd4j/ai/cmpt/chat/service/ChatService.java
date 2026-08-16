package io.ddd4j.ai.cmpt.chat.service;

import reactor.core.publisher.Flux;

/**
 * 对话端口：单轮 / 多轮（会话记忆）/ 流式对话的统一入口。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface ChatService {

    /**
     * 单轮对话。
     *
     * @param message 用户消息
     * @return 模型回答
     */
    String chat(String message);

    /**
     * 多轮对话：以 conversationId 关联会话记忆，本轮问答自动写回记忆。
     *
     * @param message         用户消息
     * @param conversationId 会话标识
     * @return 模型回答
     */
    String chat(String message, String conversationId);

    /**
     * 流式对话：增量返回模型输出。
     *
     * @param message 用户消息
     * @return 输出内容流
     */
    Flux<String> streamChat(String message);
}
