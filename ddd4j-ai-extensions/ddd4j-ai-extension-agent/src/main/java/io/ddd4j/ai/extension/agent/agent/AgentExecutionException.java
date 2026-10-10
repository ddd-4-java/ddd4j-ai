package io.ddd4j.ai.extension.agent.agent;

/**
 * Agentscope 智能体执行异常（包装底层 ReAct/工具调用失败）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class AgentExecutionException extends RuntimeException {

    /**
     * 构造仅带错误描述的执行异常。
     *
     * @param message 异常描述信息
     */
    public AgentExecutionException(String message) {
        super(message);
    }

    /**
     * 构造携带原始异常的执行异常。
     *
     * @param message 异常描述信息
     * @param cause   底层 ReAct 循环或工具调用抛出的原始异常
     */
    public AgentExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
