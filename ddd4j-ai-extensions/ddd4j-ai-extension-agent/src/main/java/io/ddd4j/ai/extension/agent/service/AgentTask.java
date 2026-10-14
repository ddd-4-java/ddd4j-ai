package io.ddd4j.ai.extension.agent.service;

import java.beans.ConstructorProperties;

import java.util.Objects;

import java.util.Map;

/**
 * 智能体任务：一次执行的输入描述。
 *
 * @param instruction    目标指令
 * @param variables      可选上下文变量（注入 prompt 用）
 * @param conversationId 可选会话标识（多轮记忆关联）
 * @author ddd4j
 */

public final class AgentTask {

    private static final long serialVersionUID = 0L;

    private final String instruction;

    private final Map<String, Object> variables;

    private final String conversationId;

    /**
     * 紧凑构造器：校验指令并防御性复制上下文变量。
     *
     * @param instruction    目标指令
     * @param variables      可选上下文变量（注入 prompt 用）
     * @param conversationId 可选会话标识（多轮记忆关联）
     * @throws IllegalArgumentException 当 {@code instruction} 为 {@code null} 或空白时抛出
     */
    @ConstructorProperties({ "instruction", "variables", "conversationId" })
    public AgentTask(String instruction, Map<String, Object> variables, String conversationId) {
        if (instruction == null || instruction.isBlank()) {
            throw new IllegalArgumentException("instruction must not be blank");
        }
        variables = variables == null ? Map.of() : Map.copyOf(variables);
        this.instruction = instruction;
        this.variables = variables;
        this.conversationId = conversationId;
    }

    /**
     * 仅以指令创建任务（无上下文变量、无会话标识）。
     *
     * @param instruction 目标指令
     * @return 新建的任务
     */
    public static AgentTask of(String instruction) {
        return new AgentTask(instruction, Map.of(), null);
    }

    public String instruction() {
        return instruction;
    }

    public Map<String, Object> variables() {
        return variables;
    }

    public String conversationId() {
        return conversationId;
    }

    public String getInstruction() {
        return instruction();
    }

    public Map<String, Object> getVariables() {
        return variables();
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
        AgentTask other = (AgentTask) obj;
        return Objects.equals(this.instruction, other.instruction) && Objects.equals(this.variables, other.variables) && Objects.equals(this.conversationId, other.conversationId);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(instruction);
        result = 31 * result + Objects.hashCode(variables);
        result = 31 * result + Objects.hashCode(conversationId);
        return result;
    }

    @Override
    public String toString() {
        return "AgentTask[instruction=" + instruction + ", variables=" + variables + ", conversationId=" + conversationId + "]";
    }
}
