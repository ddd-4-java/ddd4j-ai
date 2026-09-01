package io.ddd4j.ai.extension.agent.agent;

/**
 * Agentscope 智能体执行异常（包装底层 ReAct/工具调用失败）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class AgentExecutionException extends RuntimeException {

    public AgentExecutionException(String message) {
        super(message);
    }

    public AgentExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
