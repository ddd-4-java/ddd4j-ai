package io.ddd4j.ai.extension.agent.dispatch;

import java.util.List;
import java.util.Optional;

/**
 * 派发任务仓储接口：首版内存实现；JDBC/MySQL 实现后续按需插入。
 *
 * @author ddd4j
 */
public interface AgentDispatchTaskRepository {

    /**
     * 新增或按 id 幂等覆盖一条派发任务。
     *
     * @param task 待保存的任务记录
     */
    void save(AgentDispatchTask task);

    /**
     * 按任务 id 查询。
     *
     * @param id 任务 id
     * @return 命中的任务记录，不存在时为 {@link Optional#empty()}
     */
    Optional<AgentDispatchTask> findById(String id);

    /**
     * 查询计划下的全部任务（按创建时间升序）。
     *
     * @param planId 计划 id
     * @return 该计划的任务列表，无记录时为空列表
     */
    List<AgentDispatchTask> findByPlanId(String planId);

    /**
     * 更新已有任务（状态与结果），实现应保证按 id 幂等。
     *
     * @param task 更新后的任务记录
     */
    void update(AgentDispatchTask task);
}
