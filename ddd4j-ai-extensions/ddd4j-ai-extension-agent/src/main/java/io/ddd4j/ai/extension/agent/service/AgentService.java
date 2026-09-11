package io.ddd4j.ai.extension.agent.service;

import io.agentscope.core.event.AgentEvent;
import reactor.core.publisher.Flux;

/**
 * 智能体编排端口：一次性执行、逐步流式、细粒度事件流三条入口。
 * <p>
 * 自 2026-09-02 起默认实现为 Agentscope {@code HarnessAgent}（薄包装），
 * 原有自实现 ReAct/Plan-Execute 已移除（基线快照见 plans/baseline-*.java）。
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

    /**
     * 细粒度事件流：原生透传 Agentscope {@link AgentEvent}（含逐字文本 delta、
     * 思考 delta、工具调用 delta），供逐字 TTS / 过程可视化等场景消费。
     *
     * <p>与 {@link #stream(AgentTask)} 的区别：{@code stream} 返回经映射的粗粒度
     * {@link AgentStep}（整段文本）；本方法返回未经映射的细粒度事件，保真度更高。
     *
     * <p>默认实现抛 {@link UnsupportedOperationException}，表示该实现不支持细粒度
     * 事件流——不会静默降级为粗粒度输出。
     *
     * @param task 智能体任务
     * @return 细粒度事件流
     */
    default Flux<AgentEvent> streamEvents(AgentTask task) {
        throw new UnsupportedOperationException(
                "streamEvents 未实现：当前 AgentService 实现不支持细粒度事件流");
    }
}
