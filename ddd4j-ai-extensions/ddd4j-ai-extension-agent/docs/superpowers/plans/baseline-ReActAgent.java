package io.ddd4j.ai.extension.agent.service.impl;

import io.ddd4j.ai.extension.agent.service.AgentResult;
import io.ddd4j.ai.extension.agent.service.AgentService;
import io.ddd4j.ai.extension.agent.service.AgentStep;
import io.ddd4j.ai.extension.agent.service.AgentTask;
import io.ddd4j.ai.extension.chat.service.ChatService;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ReAct 智能体：Thought → Action → Observation 循环。
 * <p>
 * 约定模型输出格式：含 {@code Action: <toolName>} 行则触发工具调用（工具入参取
 * {@code Action Input: <json>} 行原样交给 {@link ToolCallback#call(String)}），
 * 否则视为最终答案。循环步数由 {@code maxIterations} 约束，超限抛异常防死循环。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class ReActAgent implements AgentService {

    private static final Pattern ACTION_PATTERN = Pattern.compile("(?m)^Action:\\s*([A-Za-z0-9_-]+)\\s*$");
    private static final Pattern ACTION_INPUT_PATTERN = Pattern.compile("(?ms)^Action Input:\\s*(.+)$");

    private final ChatService chatService;
    private final List<ToolCallback> toolCallbacks;
    private final int maxIterations;

    public ReActAgent(ChatService chatService, List<ToolCallback> toolCallbacks, int maxIterations) {
        this.chatService = chatService;
        this.toolCallbacks = toolCallbacks == null ? List.of() : List.copyOf(toolCallbacks);
        this.maxIterations = maxIterations;
    }

    @Override
    public AgentResult execute(AgentTask task) throws Exception {
        List<AgentStep> steps = new ArrayList<>();
        String message = task.instruction();
        for (int i = 0; i < maxIterations; i++) {
            String response = chatService.chat(message, task.conversationId());
            steps.add(new AgentStep("thought", response, steps.size()));
            String action = extractAction(response);
            if (action == null) {
                steps.add(new AgentStep("result", response, steps.size()));
                return new AgentResult(response, steps, task.conversationId());
            }
            String actionInput = extractActionInput(response);
            String observation = invokeTool(action, actionInput);
            steps.add(new AgentStep("observation", observation, steps.size()));
            message = "继续任务：" + task.instruction()
                    + "\n工具结果（Action=" + action + "）：" + observation
                    + "\n请给出下一步 Action 或最终答案。";
        }
        throw new IllegalStateException("Agent 超过最大迭代次数 " + maxIterations);
    }

    @Override
    public Flux<AgentStep> stream(AgentTask task) {
        return Flux.defer(() -> {
            try {
                return Flux.fromIterable(execute(task).steps());
            } catch (Exception e) {
                return Flux.error(e);
            }
        });
    }

    private String invokeTool(String action, String actionInput) {
        return toolCallbacks.stream()
                .filter(tool -> tool.getToolDefinition().name().equals(action))
                .findFirst()
                .map(callback -> {
                    try {
                        return callback.call(actionInput == null ? "{}" : actionInput);
                    } catch (Exception e) {
                        return "工具执行失败: " + e.getMessage();
                    }
                })
                .orElse("未知工具: " + action + "。可用工具: " + availableToolNames());
    }

    private String availableToolNames() {
        return toolCallbacks.stream()
                .map(tool -> tool.getToolDefinition().name())
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static String extractAction(String response) {
        Matcher matcher = ACTION_PATTERN.matcher(response);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String extractActionInput(String response) {
        Matcher matcher = ACTION_INPUT_PATTERN.matcher(response);
        return matcher.find() ? matcher.group(1).strip() : null;
    }
}
