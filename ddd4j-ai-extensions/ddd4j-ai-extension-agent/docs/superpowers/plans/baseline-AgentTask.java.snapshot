package io.ddd4j.ai.extension.agent.service;

import java.util.Map;

/**
 * 智能体任务：一次执行的输入描述。
 *
 * @param instruction    目标指令
 * @param variables      可选上下文变量（注入 prompt 用）
 * @param conversationId 可选会话标识（多轮记忆关联）
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public record AgentTask(String instruction, Map<String, Object> variables, String conversationId) {

    public AgentTask {
        if (instruction == null || instruction.isBlank()) {
            throw new IllegalArgumentException("instruction must not be blank");
        }
        variables = variables == null ? Map.of() : Map.copyOf(variables);
    }

    public static AgentTask of(String instruction) {
        return new AgentTask(instruction, Map.of(), null);
    }
}
