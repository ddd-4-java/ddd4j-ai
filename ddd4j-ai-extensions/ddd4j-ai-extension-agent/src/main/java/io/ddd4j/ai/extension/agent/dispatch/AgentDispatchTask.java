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
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public record AgentDispatchTask(
        String id,
        String planId,
        String instruction,
        String status,
        String result,
        Instant createdAt) {

    /** 状态常量：待执行。 */
    public static final String PENDING = "PENDING";
    /** 状态常量：执行中。 */
    public static final String RUNNING = "RUNNING";
    /** 状态常量：执行成功。 */
    public static final String DONE = "DONE";
    /** 状态常量：执行失败。 */
    public static final String FAILED = "FAILED";

    /**
     * 创建一条处于 {@link #PENDING} 状态的新派发任务，自动生成 UUID 与创建时间。
     *
     * @param planId      所属计划 id
     * @param instruction 子任务指令
     * @return 状态为 PENDING 的派发任务记录
     */
    public static AgentDispatchTask pending(String planId, String instruction) {
        return new AgentDispatchTask(java.util.UUID.randomUUID().toString(), planId,
                instruction, PENDING, null, Instant.now());
    }

    /**
     * 基于当前记录派生一条状态与结果更新的新记录（不可变更新语义，创建时间保持不变）。
     *
     * @param newStatus 新状态（RUNNING / DONE / FAILED 等）
     * @param newResult 新结果（DONE 时为子智能体回答；FAILED 时为错误消息）
     * @return 状态与结果更新后的派发任务记录
     */
    public AgentDispatchTask withStatus(String newStatus, String newResult) {
        return new AgentDispatchTask(id, planId, instruction, newStatus, newResult, createdAt);
    }
}
