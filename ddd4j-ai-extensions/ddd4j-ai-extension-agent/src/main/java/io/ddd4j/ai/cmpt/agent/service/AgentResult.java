package io.ddd4j.ai.cmpt.agent.service;

import java.util.List;

/**
 * 智能体执行结果：最终输出 + 完整步骤轨迹。
 *
 * @param output         最终答案
 * @param steps          执行轨迹（thought/action/observation/result）
 * @param conversationId 会话标识（与任务一致）
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public record AgentResult(String output, List<AgentStep> steps, String conversationId) {

    public AgentResult {
        steps = List.copyOf(steps);
    }
}
