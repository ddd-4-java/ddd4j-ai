package io.ddd4j.ai.extension.agent.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.AssistantMessage;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import io.ddd4j.ai.extension.agent.service.AgentResult;
import io.ddd4j.ai.extension.agent.service.AgentService;
import io.ddd4j.ai.extension.agent.service.AgentStep;
import io.ddd4j.ai.extension.agent.service.AgentTask;
import io.ddd4j.ai.extension.agent.util.BlockingCallGuard;
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
        BlockingCallGuard.requireBlockingCapableThread("executeAsync(task)");
        try {
            Msg response = harnessAgent.call(new UserMessage(task.instruction())).block();
            return toResult(response, task.conversationId());
        } catch (RuntimeException e) {
            log.warn("agentscope execute failed: {}", e.getMessage());
            throw new AgentExecutionException("agentscope execute failed: " + e.getMessage(), e);
        }
    }

    /**
     * 零阻塞执行：直接复用 Agentscope 的 reactive 链，全程不经过 {@code .block()}。
     *
     * <p>与 {@link #execute(AgentTask)} 的区别：本方法可在 Reactor 非阻塞线程上安全调用，
     * 是 WebFlux / Reactor 消费方的正规入口。
     */
    @Override
    public Mono<AgentResult> executeAsync(AgentTask task) {
        Objects.requireNonNull(task, "task");
        return harnessAgent.call(new UserMessage(task.instruction()))
                .map(response -> toResult(response, task.conversationId()))
                .onErrorMap(RuntimeException.class, e -> {
                    log.warn("agentscope executeAsync failed: {}", e.getMessage());
                    return new AgentExecutionException(
                            "agentscope execute failed: " + e.getMessage(), e);
                });
    }

    /**
     * 把 Agentscope 响应规整为 {@link AgentResult}。
     *
     * <p>同步 {@link #execute(AgentTask)} 与异步 {@link #executeAsync(AgentTask)} 共用本方法，
     * 以保证两条路径产出完全一致。
     */
    private static AgentResult toResult(Msg response, String conversationId) {
        List<AgentStep> steps = new ArrayList<>();
        if (response instanceof AssistantMessage assistant) {
            String text = extractText(assistant);
            steps.add(new AgentStep("result", text, 0));
            return new AgentResult(text, steps, conversationId);
        }
        String fallback = response == null ? "" : response.getClass().getSimpleName();
        steps.add(new AgentStep("result", fallback, 0));
        return new AgentResult(fallback, steps, conversationId);
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

    /**
     * 细粒度事件流：直接委托 Agentscope {@link HarnessAgent#streamEvents}，
     * 原生透传 {@link AgentEvent}（逐字 delta / 思考 delta / 工具调用 delta）。
     *
     * <p>与 {@link #stream(AgentTask)} 的粗粒度 {@code io.agentscope.core.agent.Event}
     * 是两套并行抽象：本方法不做事件类型映射，保真度最高。
     */
    @Override
    public Flux<AgentEvent> streamEvents(AgentTask task) {
        Objects.requireNonNull(task, "task");
        return harnessAgent.streamEvents(new UserMessage(task.instruction()))
                .onErrorMap(RuntimeException.class, e -> {
                    log.warn("agentscope streamEvents failed: {}", e.getMessage());
                    return new AgentExecutionException(
                            "agentscope streamEvents failed: " + e.getMessage(), e);
                });
    }
}
