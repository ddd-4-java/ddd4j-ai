package io.ddd4j.ai.extension.agent.dispatch;

import java.util.List;
import java.util.Optional;

/**
 * 派发任务仓储接口：首版内存实现；JDBC/MySQL 实现后续按需插入。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface AgentDispatchTaskRepository {

    void save(AgentDispatchTask task);

    Optional<AgentDispatchTask> findById(String id);

    List<AgentDispatchTask> findByPlanId(String planId);

    void update(AgentDispatchTask task);
}
