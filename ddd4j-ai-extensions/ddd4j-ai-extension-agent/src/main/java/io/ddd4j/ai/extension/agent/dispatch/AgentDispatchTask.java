package io.ddd4j.ai.extension.agent.dispatch;

import java.beans.ConstructorProperties;

import java.util.Objects;

import java.time.Instant;

/**
 * 派发任务记录：一个计划（plan）下的一行子任务。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */

public final class AgentDispatchTask {

    private static final long serialVersionUID = 0L;

    private final String id;

    private final String planId;

    private final String instruction;

    private final String status;

    private final String result;

    private final Instant createdAt;

    /**
     * 状态常量：待执行。
     */
    public static final String PENDING = "PENDING";

    /**
     * 状态常量：执行中。
     */
    public static final String RUNNING = "RUNNING";

    /**
     * 状态常量：执行成功。
     */
    public static final String DONE = "DONE";

    /**
     * 状态常量：执行失败。
     */
    public static final String FAILED = "FAILED";

    /**
     * 创建一条处于 {@link #PENDING} 状态的新派发任务，自动生成 UUID 与创建时间。
     *
     * @param planId      所属计划 id
     * @param instruction 子任务指令
     * @return 状态为 PENDING 的派发任务记录
     */
    public static AgentDispatchTask pending(String planId, String instruction) {
        return new AgentDispatchTask(java.util.UUID.randomUUID().toString(), planId, instruction, PENDING, null, Instant.now());
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

    /**
 * @param id 任务 id（UUID）
 * @param planId 所属计划 id
 * @param instruction 子任务指令（子智能体的输入）
 * @param status 状态：PENDING / RUNNING / DONE / FAILED
 * @param result 执行结果（DONE 时为子智能体回答；FAILED 时为错误消息）
 * @param createdAt 创建时间
 */

    @ConstructorProperties({ "id", "planId", "instruction", "status", "result", "createdAt" })
    public AgentDispatchTask(String id, String planId, String instruction, String status, String result, Instant createdAt) {
        this.id = id;
        this.planId = planId;
        this.instruction = instruction;
        this.status = status;
        this.result = result;
        this.createdAt = createdAt;
    }

    public String id() {
        return id;
    }

    public String planId() {
        return planId;
    }

    public String instruction() {
        return instruction;
    }

    public String status() {
        return status;
    }

    public String result() {
        return result;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String getId() {
        return id();
    }

    public String getPlanId() {
        return planId();
    }

    public String getInstruction() {
        return instruction();
    }

    public String getStatus() {
        return status();
    }

    public String getResult() {
        return result();
    }

    public Instant getCreatedAt() {
        return createdAt();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        AgentDispatchTask other = (AgentDispatchTask) obj;
        return Objects.equals(this.id, other.id) && Objects.equals(this.planId, other.planId) && Objects.equals(this.instruction, other.instruction) && Objects.equals(this.status, other.status) && Objects.equals(this.result, other.result) && Objects.equals(this.createdAt, other.createdAt);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(planId);
        result = 31 * result + Objects.hashCode(instruction);
        result = 31 * result + Objects.hashCode(status);
        result = 31 * result + Objects.hashCode(result);
        result = 31 * result + Objects.hashCode(createdAt);
        return result;
    }

    @Override
    public String toString() {
        return "AgentDispatchTask[id=" + id + ", planId=" + planId + ", instruction=" + instruction + ", status=" + status + ", result=" + result + ", createdAt=" + createdAt + "]";
    }
}
