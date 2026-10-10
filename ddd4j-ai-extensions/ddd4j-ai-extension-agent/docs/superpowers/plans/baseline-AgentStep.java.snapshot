package io.ddd4j.ai.extension.agent.service;

/**
 * 智能体执行中的一步：思考 / 动作 / 观察 / 结果（流式与诊断用）。
 *
 * @param type    步骤类型：plan / thought / action / observation / execution / result
 * @param content 步骤内容
 * @param index   步骤序号（0 起）
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public record AgentStep(String type, String content, int index) {

    public AgentStep {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
    }
}
