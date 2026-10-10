package io.ddd4j.ai.extension.agent.dispatch;

import java.util.List;
import java.util.Optional;

/**
 * 派发任务仓储接口：首版内存实现；JDBC/MySQL 实现后续按需插入。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface AgentDispatchTaskRepository {

    /**
     * 保存派发任务（新增）。
     *
     * @param task 派发任务
     */
    void save(AgentDispatchTask task);

    /**
     * 按任务 ID 查询派发任务。
     *
     * @param id 任务 ID
     * @return 命中的任务；不存在时为空
     */
    Optional<AgentDispatchTask> findById(String id);

    /**
     * 按执行计划 ID 查询其下全部派发任务。
     *
     * @param planId 执行计划 ID
     * @return 该计划的任务列表（无任务时为空列表）
     */
    List<AgentDispatchTask> findByPlanId(String planId);

    /**
     * 更新既有派发任务状态/结果。
     *
     * @param task 派发任务（须已存在）
     */
    void update(AgentDispatchTask task);
}
