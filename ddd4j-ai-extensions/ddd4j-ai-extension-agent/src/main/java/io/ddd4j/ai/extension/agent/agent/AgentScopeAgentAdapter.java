package io.ddd4j.ai.extension.agent.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.AssistantMessage;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import io.ddd4j.ai.extension.agent.service.AgentResult;
import io.ddd4j.ai.extension.agent.service.AgentService;
import io.ddd4j.ai.extension.agent.service.AgentStep;
import io.ddd4j.ai.extension.agent.service.AgentTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * ddd4j-ai-extension-agent 的 {@link AgentService} SPI 实现：把 Agentscope {@link HarnessAgent}
 * 包装为业务友好的 {@link AgentResult}/{@link AgentStep}。
 * <p>
 * 参考 cloud-agents 的 `AgentScopeHarnessFactory` 模式 — 业务方构造 HarnessAgent（注入
 * Model/Toolkit/Memory/Subagents），本类仅做流式到 POJO 的桥接。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public final class AgentScopeAgentAdapter implements AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentScopeAgentAdapter.class);

    private final HarnessAgent harnessAgent;

    public AgentScopeAgentAdapter(HarnessAgent harnessAgent) {
        this.harnessAgent = Objects.requireNonNull(harnessAgent, "harnessAgent");
    }

    @Override
    public AgentResult execute(AgentTask task) {
        Objects.requireNonNull(task, "task");
        List<AgentStep> steps = new ArrayList<>();
        try {
            Msg response = harnessAgent.call(new UserMessage(task.instruction())).block();
            if (response instanceof AssistantMessage assistant) {
                steps.add(new AgentStep("result", extractText(assistant), steps.size()));
                return new AgentResult(extractText(assistant), steps, task.conversationId());
            }
            String fallback = response == null ? "" : response.getClass().getSimpleName();
            steps.add(new AgentStep("result", fallback, steps.size()));
            return new AgentResult(fallback, steps, task.conversationId());
        } catch (RuntimeException e) {
            log.warn("agentscope execute failed: {}", e.getMessage());
            throw new AgentExecutionException("agentscope execute failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<AgentStep> stream(AgentTask task) {
        Objects.requireNonNull(task, "task");
        return harnessAgent.stream(new UserMessage(task.instruction()), StreamOptions.defaults())
                .map(this::toStep)
                .onErrorMap(RuntimeException.class, e -> {
                    log.warn("agentscope stream failed: {}", e.getMessage());
                    return new AgentExecutionException("agentscope stream failed: " + e.getMessage(), e);
                });
    }

    private AgentStep toStep(Event event) {
        String type = switch (event.getType()) {
            case REASONING -> "thought";
            case TOOL_RESULT -> "observation";
            case HINT -> "hint";
            case AGENT_RESULT -> "result";
            case SUMMARY -> "summary";
            default -> "event";
        };
        String content = event.getMessage() == null ? "" : event.getMessage().getTextContent();
        return new AgentStep(type, content == null ? "" : content, Math.abs(content.hashCode() % 10000));
    }

    private static String extractText(Msg msg) {
        if (msg == null) {
            return "";
        }
        String text = msg.getTextContent();
        return text == null ? "" : text;
    }
}
