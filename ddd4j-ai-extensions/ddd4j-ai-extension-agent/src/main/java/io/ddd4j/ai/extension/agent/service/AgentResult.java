package io.ddd4j.ai.extension.agent.service;

import java.util.List;

/**
 * 智能体执行结果：最终输出 + 完整步骤轨迹。
 *
 * @param output         最终答案
 * @param steps          执行轨迹（thought/action/observation/result）
 * @param conversationId 会话标识（与任务一致）
 * @author ddd4j
 */
public record AgentResult(String output, List<AgentStep> steps, String conversationId) {

    /**
     * 紧凑构造器：对执行轨迹做防御性复制。
     */
    public AgentResult {
        steps = List.copyOf(steps);
    }
}
