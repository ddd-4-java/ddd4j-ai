package io.ddd4j.ai.extension.agent.service;

import reactor.core.publisher.Flux;

/**
 * 智能体编排端口：一次性执行与逐步流式两条入口，屏蔽具体策略（ReAct / Plan-Execute）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface AgentService {

    /**
     * 执行任务并返回最终结果与完整步骤轨迹。
     *
     * @param task 智能体任务
     * @return 执行结果
     * @throws Exception 超过最大迭代 / 引擎失败
     */
    AgentResult execute(AgentTask task) throws Exception;

    /**
     * 流式执行：逐步下发思考/动作/观察/结果步骤。
     *
     * @param task 智能体任务
     * @return 步骤流
     */
    Flux<AgentStep> stream(AgentTask task);
}
