package io.ddd4j.ai.extension.agent.service.impl;

import io.ddd4j.ai.extension.agent.service.AgentResult;
import io.ddd4j.ai.extension.agent.service.AgentService;
import io.ddd4j.ai.extension.agent.service.AgentStep;
import io.ddd4j.ai.extension.agent.service.AgentTask;
import io.ddd4j.ai.extension.chat.service.ChatService;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Plan-Execute 智能体：先让模型产出分步计划，再逐条执行并聚合各步结果。
 * <p>
 * 计划解析支持 {@code 1. item}、{@code - item}、{@code * item} 行前缀，
 * 步数由 {@code maxPlanSteps} 约束。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class PlanExecuteAgent implements AgentService {

    private static final Pattern PLAN_ITEM_PATTERN =
            Pattern.compile("^\\s*(?:\\d+[.、)）]|[-*•])\\s*(.+?)\\s*$");

    private final ChatService chatService;
    private final int maxPlanSteps;

    public PlanExecuteAgent(ChatService chatService, int maxPlanSteps) {
        this.chatService = chatService;
        this.maxPlanSteps = maxPlanSteps;
    }

    @Override
    public AgentResult execute(AgentTask task) throws Exception {
        String plan = chatService.chat("为以下目标制定分步计划（每行一条，编号或列表前缀）：\n" + task.instruction(),
                task.conversationId());
        List<AgentStep> steps = new ArrayList<>();
        steps.add(new AgentStep("plan", plan, 0));

        List<String> planItems = extractPlanItems(plan);
        if (planItems.isEmpty()) {
            steps.add(new AgentStep("result", plan, 1));
            return new AgentResult(plan, steps, task.conversationId());
        }

        StringBuilder output = new StringBuilder();
        for (int i = 0; i < planItems.size(); i++) {
            String item = planItems.get(i);
            String result = chatService.chat("执行计划步骤（目标：" + task.instruction() + "）：\n" + item,
                    task.conversationId());
            steps.add(new AgentStep("execution", result, steps.size()));
            output.append(result).append('\n');
        }
        String finalOutput = output.toString().strip();
        steps.add(new AgentStep("result", finalOutput, steps.size()));
        return new AgentResult(finalOutput, steps, task.conversationId());
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

    private List<String> extractPlanItems(String plan) {
        return plan.lines()
                .map(line -> {
                    var matcher = PLAN_ITEM_PATTERN.matcher(line);
                    return matcher.matches() ? matcher.group(1).strip() : "";
                })
                .filter(item -> !item.isEmpty())
                .limit(maxPlanSteps)
                .toList();
    }
}
