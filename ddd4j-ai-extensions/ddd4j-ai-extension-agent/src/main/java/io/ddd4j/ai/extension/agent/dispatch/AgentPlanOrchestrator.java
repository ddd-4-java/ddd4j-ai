package io.ddd4j.ai.extension.agent.dispatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import io.ddd4j.ai.extension.agent.agent.AgentExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 多智能体计划派发编排器：提交计划 → 拆分为子任务行 → 并行派发 HarnessAgent 执行 →
 * 结果写回任务表 → 全部完成后父智能体合并。
 *
 * <p>这是「自主规划→拆分→派发→合并」引擎的执行半边；计划的产生（plan mode / LLM 结构化输出）
 * 由调用方（或 Agentscope PlanMode）负责，本类消费结构化的任务指令列表。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class AgentPlanOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentPlanOrchestrator.class);

    private final HarnessAgent harnessAgent;
    private final AgentDispatchTaskRepository repository;

    public AgentPlanOrchestrator(HarnessAgent harnessAgent, AgentDispatchTaskRepository repository) {
        this.harnessAgent = Objects.requireNonNull(harnessAgent, "harnessAgent");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * 提交计划：为每条任务指令创建 PENDING 任务行。
     *
     * @return planId
     */
    public String submitPlan(String goal, List<String> taskInstructions) {
        Objects.requireNonNull(goal, "goal");
        if (taskInstructions == null || taskInstructions.isEmpty()) {
            throw new IllegalArgumentException("taskInstructions must not be empty");
        }
        String planId = java.util.UUID.randomUUID().toString();
        for (String instruction : taskInstructions) {
            if (instruction == null || instruction.isBlank()) {
                continue;
            }
            AgentDispatchTask task = AgentDispatchTask.pending(planId,
                    "目标：" + goal + "\n任务：" + instruction.strip());
            repository.save(task);
        }
        log.info("plan submitted: planId={}, tasks={}", planId, taskInstructions.size());
        return planId;
    }

    /**
     * 并行派发该计划下全部 PENDING 任务给 HarnessAgent，结果写回任务表。
     *
     * @return planId → 各任务执行结果（taskId → result/error）
     */
    public Map<String, String> dispatchAll(String planId) {
        Objects.requireNonNull(planId, "planId");
        List<AgentDispatchTask> tasks = repository.findByPlanId(planId).stream()
                .filter(t -> AgentDispatchTask.PENDING.equals(t.status()))
                .toList();
        if (tasks.isEmpty()) {
            throw new AgentExecutionException("no pending tasks for plan: " + planId);
        }
        tasks.forEach(t -> repository.update(t.withStatus(AgentDispatchTask.RUNNING, null)));

        Map<String, Mono<AgentDispatchTask>> executions = new HashMap<>();
        for (AgentDispatchTask task : tasks) {
            executions.put(task.id(), executeTask(task));
        }
        List<AgentDispatchTask> finished = Flux.merge(executions.values())
                .collectList()
                .block();
        Map<String, String> results = new java.util.LinkedHashMap<>();
        for (AgentDispatchTask task : finished == null ? List.<AgentDispatchTask>of() : finished) {
            repository.update(task);
            results.put(task.id(), task.result());
        }
        log.info("plan dispatched: planId={}, done={}, failed={}", planId,
                results.size(), 0);
        return results;
    }

    /** 查看计划下全部任务（诊断/测试用）。 */
    public List<AgentDispatchTask> tasksOf(String planId) {
        return List.copyOf(repository.findByPlanId(planId));
    }

    /**
     * 合并结果：全部 DONE 时让父智能体 synthesis；有 FAILED 抛异常。
     */
    public String mergeResults(String planId) {
        List<AgentDispatchTask> tasks = repository.findByPlanId(planId);
        if (tasks.isEmpty()) {
            throw new AgentExecutionException("no tasks for plan: " + planId);
        }
        for (AgentDispatchTask task : tasks) {
            if (!AgentDispatchTask.DONE.equals(task.status())) {
                throw new AgentExecutionException("plan not fully done: " + planId
                        + ", task " + task.id() + " is " + task.status());
            }
        }
        StringBuilder prompt = new StringBuilder("合并以下子任务结果为一个最终答案：\n");
        for (AgentDispatchTask task : tasks) {
            prompt.append("- ").append(task.instruction().replace('\n', ' '))
                    .append("\n  结果：").append(task.result()).append('\n');
        }
        Msg merged = harnessAgent.call(new UserMessage(prompt.toString())).block();
        return merged == null ? "" : merged.getTextContent();
    }

    private Mono<AgentDispatchTask> executeTask(AgentDispatchTask task) {
        return Mono.fromCallable(() -> harnessAgent.call(new UserMessage(task.instruction())).block())
                .map(response -> task.withStatus(AgentDispatchTask.DONE,
                        response == null ? "" : response.getTextContent()))
                .onErrorResume(e -> {
                    log.warn("task dispatch failed: id={}, error={}", task.id(), e.getMessage());
                    return Mono.just(task.withStatus(AgentDispatchTask.FAILED,
                            String.valueOf(e.getMessage())));
                });
    }
}
