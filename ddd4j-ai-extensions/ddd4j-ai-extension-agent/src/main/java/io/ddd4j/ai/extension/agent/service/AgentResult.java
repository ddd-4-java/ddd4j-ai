package io.ddd4j.ai.extension.agent.service;

import java.beans.ConstructorProperties;

import java.util.Objects;

import java.util.List;

/**
 * 智能体执行结果：最终输出 + 完整步骤轨迹。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */

public final class AgentResult {

    private static final long serialVersionUID = 0L;

    private final String output;

    private final List<AgentStep> steps;

    private final String conversationId;

    /**
 * 规范构造器：把步骤轨迹固化为不可变列表。
 *
 * @param output 最终答案
 * @param steps 执行轨迹（thought/action/observation/result）
 * @param conversationId 会话标识（与任务一致）
 */

    @ConstructorProperties({ "output", "steps", "conversationId" })
    public AgentResult(String output, List<AgentStep> steps, String conversationId) {
        steps = List.copyOf(steps);
        this.output = output;
        this.steps = steps;
        this.conversationId = conversationId;
    }

    public String output() {
        return output;
    }

    public List<AgentStep> steps() {
        return steps;
    }

    public String conversationId() {
        return conversationId;
    }

    public String getOutput() {
        return output();
    }

    public List<AgentStep> getSteps() {
        return steps();
    }

    public String getConversationId() {
        return conversationId();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        AgentResult other = (AgentResult) obj;
        return Objects.equals(this.output, other.output) && Objects.equals(this.steps, other.steps) && Objects.equals(this.conversationId, other.conversationId);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(output);
        result = 31 * result + Objects.hashCode(steps);
        result = 31 * result + Objects.hashCode(conversationId);
        return result;
    }

    @Override
    public String toString() {
        return "AgentResult[output=" + output + ", steps=" + steps + ", conversationId=" + conversationId + "]";
    }
}
