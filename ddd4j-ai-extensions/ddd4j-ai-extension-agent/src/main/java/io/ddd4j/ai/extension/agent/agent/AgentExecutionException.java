package io.ddd4j.ai.extension.agent.agent;

/**
 * Agentscope 智能体执行异常（包装底层 ReAct/工具调用失败）。
 *
 * @author ddd4j
 */
public class AgentExecutionException extends RuntimeException {

    /**
     * 创建仅带错误消息的执行异常。
     *
     * @param message 错误消息
     */
    public AgentExecutionException(String message) {
        super(message);
    }

    /**
     * 创建携带原始异常 cause 的执行异常。
     *
     * @param message 错误消息
     * @param cause   底层失败原因
     */
    public AgentExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
