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
import io.ddd4j.ai.extension.agent.util.BlockingCallGuard;
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
     * 非阻塞派发：为 WebFlux / Reactor 消费方提供的正规入口。全程不经过 {@code .block()}。
     *
     * @return planId → 各任务执行结果（taskId → result/error）
     */
    public Mono<Map<String, String>> dispatchAllAsync(String planId) {
        Objects.requireNonNull(planId, "planId");
        return Mono.fromCallable(() -> {
                    List<AgentDispatchTask> pending = repository.findByPlanId(planId).stream()
                            .filter(t -> AgentDispatchTask.PENDING.equals(t.status()))
                            .toList();
                    if (pending.isEmpty()) {
                        throw new AgentExecutionException("no pending tasks for plan: " + planId);
                    }
                    pending.forEach(t -> repository.update(t.withStatus(AgentDispatchTask.RUNNING, null)));
                    return pending;
                })
                .flatMap(tasks -> Flux.merge(tasks.stream().map(this::executeTask).toList())
                        .collectList())
                .map(finished -> {
                    Map<String, String> results = new java.util.LinkedHashMap<>();
                    for (AgentDispatchTask task : finished) {
                        repository.update(task);
                        results.put(task.id(), task.result());
                    }
                    log.info("plan dispatched: planId={}, done={}", planId, results.size());
                    return results;
                });
    }

    /**
     * 并行派发该计划下全部 PENDING 任务给 HarnessAgent，结果写回任务表。
     *
     * <p>同步入口：在 Reactor 非阻塞线程上会抛出带指引的异常，请改用
     * {@link #dispatchAllAsync(String)}。
     *
     * @return planId → 各任务执行结果（taskId → result/error）
     */
    public Map<String, String> dispatchAll(String planId) {
        BlockingCallGuard.requireBlockingCapableThread("dispatchAllAsync(planId)");
        return dispatchAllAsync(planId).block();
    }

    /** 查看计划下全部任务（诊断/测试用）。 */
    public List<AgentDispatchTask> tasksOf(String planId) {
        return List.copyOf(repository.findByPlanId(planId));
    }

    /**
     * 非阻塞合并：全部 DONE 时让父智能体 synthesis；有非 DONE 任务则错误终止。
     */
    public Mono<String> mergeResultsAsync(String planId) {
        Objects.requireNonNull(planId, "planId");
        return Mono.fromCallable(() -> {
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
                    return prompt.toString();
                })
                .flatMap(prompt -> harnessAgent.call(new UserMessage(prompt)))
                .map(merged -> merged == null ? "" : merged.getTextContent());
    }

    /**
     * 合并结果（同步入口）：在 Reactor 非阻塞线程上会抛出带指引的异常，
     * 请改用 {@link #mergeResultsAsync(String)}。
     */
    public String mergeResults(String planId) {
        BlockingCallGuard.requireBlockingCapableThread("mergeResultsAsync(planId)");
        return mergeResultsAsync(planId).block();
    }

    private Mono<AgentDispatchTask> executeTask(AgentDispatchTask task) {
        return harnessAgent.call(new UserMessage(task.instruction()))
                .map(response -> task.withStatus(AgentDispatchTask.DONE,
                        response == null ? "" : response.getTextContent()))
                .onErrorResume(e -> {
                    log.warn("task dispatch failed: id={}, error={}", task.id(), e.getMessage());
                    return Mono.just(task.withStatus(AgentDispatchTask.FAILED,
                            String.valueOf(e.getMessage())));
                });
    }
}
