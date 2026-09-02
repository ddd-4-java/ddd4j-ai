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

    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String DONE = "DONE";
    public static final String FAILED = "FAILED";

    public static AgentDispatchTask pending(String planId, String instruction) {
        return new AgentDispatchTask(java.util.UUID.randomUUID().toString(), planId,
                instruction, PENDING, null, Instant.now());
    }

    public AgentDispatchTask withStatus(String newStatus, String newResult) {
        return new AgentDispatchTask(id, planId, instruction, newStatus, newResult, createdAt);
    }
}
