package io.ddd4j.ai.extension.agent.dispatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版派发任务仓储（首版默认实现；生产建议换 JDBC/MySQL 实现）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class InMemoryAgentDispatchTaskRepository implements AgentDispatchTaskRepository {

    /**
     * 构造 InMemoryAgentDispatchTaskRepository 对象。
     */
    public InMemoryAgentDispatchTaskRepository() {
    }

    private final Map<String, AgentDispatchTask> store = new ConcurrentHashMap<>();

    /**
     * 保存（新增或整体覆盖）一条派发任务记录。
     *
     * @param task 待保存的派发任务
     */
    @Override
    public void save(AgentDispatchTask task) {
        store.put(task.id(), task);
    }

    /**
     * 按任务 id 查询派发任务。
     *
     * @param id 任务 id
     * @return 命中的派发任务；不存在时为 {@link Optional#empty()}
     */
    @Override
    public Optional<AgentDispatchTask> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    /**
     * 查询指定计划下的全部派发任务，按创建时间升序返回。
     *
     * @param planId 计划 id
     * @return 该计划的派发任务不可变列表；无记录时为空列表
     */
    @Override
    public List<AgentDispatchTask> findByPlanId(String planId) {
        List<AgentDispatchTask> result = new ArrayList<>();
        for (AgentDispatchTask task : store.values()) {
            if (task.planId().equals(planId)) {
                result.add(task);
            }
        }
        result.sort((a, b) -> a.createdAt().compareTo(b.createdAt()));
        return List.copyOf(result);
    }

    /**
     * 更新既有派发任务记录（按 id 覆盖，等价于 {@link #save}）。
     *
     * @param task 更新后的派发任务
     */
    @Override
    public void update(AgentDispatchTask task) {
        store.put(task.id(), task);
    }
}
