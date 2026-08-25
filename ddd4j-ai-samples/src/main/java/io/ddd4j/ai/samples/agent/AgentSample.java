package io.ddd4j.ai.samples.agent;

import io.ddd4j.ai.cmpt.agent.service.AgentResult;
import io.ddd4j.ai.cmpt.agent.service.AgentService;
import io.ddd4j.ai.cmpt.agent.service.AgentStep;
import io.ddd4j.ai.cmpt.agent.service.AgentTask;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * agent 智能体编排组件使用示例：ReAct 工具调用循环 / 流式步骤。
 *
 * <p>前提：引入 {@code ddd4j-ai-extension-agent}，并依赖 chat 组件（模型 starter）；
 * 工具回调由业务侧注册 {@code ToolCallback} bean 注入（如 Spring AI 的
 * {@code MethodToolCallbackProvider}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Component
public class AgentSample {

    private final AgentService agentService;

    public AgentSample(AgentService agentService) {
        this.agentService = agentService;
    }

    /** 一次性执行：返回最终答案与完整思考/动作轨迹。 */
    public AgentResult run(String instruction) throws Exception {
        return agentService.execute(AgentTask.of(instruction));
    }

    /** 流式执行：逐步消费 thought / observation / result 步骤。 */
    public Flux<AgentStep> stream(String instruction) {
        return agentService.stream(AgentTask.of(instruction));
    }
}
