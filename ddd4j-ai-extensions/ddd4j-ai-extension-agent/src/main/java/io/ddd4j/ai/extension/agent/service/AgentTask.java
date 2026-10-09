package io.ddd4j.ai.extension.agent.service;

import java.util.Map;

/**
 * 智能体任务：一次执行的输入描述。
 *
 * @param instruction    目标指令
 * @param variables      可选上下文变量（注入 prompt 用）
 * @param conversationId 可选会话标识（多轮记忆关联）
 * @author ddd4j
 */
public record AgentTask(String instruction, Map<String, Object> variables, String conversationId) {

    /**
     * 紧凑构造器：校验指令并防御性复制上下文变量。
     *
     * @throws IllegalArgumentException 当 {@code instruction} 为 {@code null} 或空白时抛出
     */
    public AgentTask {
        if (instruction == null || instruction.isBlank()) {
            throw new IllegalArgumentException("instruction must not be blank");
        }
        variables = variables == null ? Map.of() : Map.copyOf(variables);
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
}
