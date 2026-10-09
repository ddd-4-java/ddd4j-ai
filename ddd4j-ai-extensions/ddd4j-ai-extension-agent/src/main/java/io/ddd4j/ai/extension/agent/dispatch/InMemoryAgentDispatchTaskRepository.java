package io.ddd4j.ai.extension.agent.dispatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版派发任务仓储（首版默认实现；生产建议换 JDBC/MySQL 实现）。
 *
 * @author ddd4j
 */
public class InMemoryAgentDispatchTaskRepository implements AgentDispatchTaskRepository {

    /**
     * 默认构造器：初始化线程安全的内存存储。
     */
    public InMemoryAgentDispatchTaskRepository() {
    }

    private final Map<String, AgentDispatchTask> store = new ConcurrentHashMap<>();

    @Override
    public void save(AgentDispatchTask task) {
        store.put(task.id(), task);
    }

    @Override
    public Optional<AgentDispatchTask> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

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

    @Override
    public void update(AgentDispatchTask task) {
        store.put(task.id(), task);
    }
}
