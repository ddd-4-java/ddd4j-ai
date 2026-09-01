package io.ddd4j.ai.samples.agent;

import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * agent 智能体组件使用示例（Agentscope 域模型）：装配好的 HarnessAgent 可直接注入，
 * 也可由业务自行用 {@code HarnessAgent.builder()} 构造定制智能体。
 *
 * <p>前提：业务服务引入 {@code ddd4j-ai-extension-agent}，并配置
 * {@code ddd4j.ai.agent.api-key} + {@code ddd4j.ai.agent.model-name}（OpenAI 兼容协议，
 * 或业务侧注入 {@code io.agentscope.core.model.Model} Bean）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Component
public class AgentSample {

    private final HarnessAgent harnessAgent;

    public AgentSample(HarnessAgent harnessAgent) {
        this.harnessAgent = harnessAgent;
    }

    /** 同步执行：返回最终回答文本。 */
    public String run(String instruction) {
        return harnessAgent.call(new UserMessage(instruction)).block().getTextContent();
    }

    /** 异步执行：返回响应式回答。 */
    public Mono<String> runAsync(String instruction) {
        return harnessAgent.call(new UserMessage(instruction))
                .map(message -> message.getTextContent());
    }
}
