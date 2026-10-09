package io.ddd4j.ai.extension.agent.dispatch;

import java.time.Instant;

/**
 * 派发任务记录：一个计划（plan）下的一行子任务。
 *
 * @param id          任务 id（UUID）
 * @param planId      所属计划 id
 * @param instruction 子任务指令（子智能体的输入）
 * @param status      状态：PENDING / RUNNING / DONE / FAILED
 * @param result      执行结果（DONE 时为子智能体回答；FAILED 时为错误消息）
 * @param createdAt   创建时间
 * @author ddd4j
 */
public record AgentDispatchTask(
        String id,
        String planId,
        String instruction,
        String status,
        String result,
        Instant createdAt) {

    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String DONE = "DONE";
    public static final String FAILED = "FAILED";

    /**
     * 创建一条 PENDING 状态的子任务记录。
     *
     * @param planId      所属计划 id
     * @param instruction 子任务指令
     * @return 携带随机 UUID 与当前时间的任务记录
     */
    public static AgentDispatchTask pending(String planId, String instruction) {
        return new AgentDispatchTask(java.util.UUID.randomUUID().toString(), planId,
                instruction, PENDING, null, Instant.now());
    }

    /**
     * 复制本任务并替换状态与结果（不可变更新）。
     *
     * @param newStatus 新状态（RUNNING / DONE / FAILED）
     * @param newResult 新结果内容，可为 {@code null}
     * @return 状态更新后的任务记录
     */
    public AgentDispatchTask withStatus(String newStatus, String newResult) {
        return new AgentDispatchTask(id, planId, instruction, newStatus, newResult, createdAt);
    }
}
