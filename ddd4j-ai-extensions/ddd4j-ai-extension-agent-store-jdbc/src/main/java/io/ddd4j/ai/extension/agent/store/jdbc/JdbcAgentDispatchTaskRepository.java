package io.ddd4j.ai.extension.agent.store.jdbc;

import io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTask;
import io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link AgentDispatchTaskRepository} 的 JDBC 实现：一次覆盖 PostgreSQL / MySQL / H2。
 *
 * <p>方言差异全部交给 {@link JdbcDialect}，本类不含任何方言分支。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class JdbcAgentDispatchTaskRepository implements AgentDispatchTaskRepository {

    private static final RowMapper<AgentDispatchTask> ROW_MAPPER = (rs, rowNum) -> new AgentDispatchTask(
            rs.getString("id"),
            rs.getString("plan_id"),
            rs.getString("instruction"),
            rs.getString("status"),
            rs.getString("result"),
            rs.getTimestamp("created_at").toInstant());

    private final JdbcTemplate jdbcTemplate;
    private final JdbcDialect dialect;

    /**
     * @param dataSource 业务提供的连接池数据源
     * @param dialect    数据库方言
     * @param autoDdl    是否自动建表（{@code false} 时由业务自行管理表结构）
     */
    public JdbcAgentDispatchTaskRepository(DataSource dataSource, JdbcDialect dialect, boolean autoDdl) {
        Objects.requireNonNull(dataSource, "dataSource");
        this.dialect = Objects.requireNonNull(dialect, "dialect");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        if (autoDdl) {
            this.jdbcTemplate.execute(dialect.createTableSql());
            this.jdbcTemplate.execute(dialect.createIndexSql());
        }
    }

    @Override
    public void save(AgentDispatchTask task) {
        jdbcTemplate.update(dialect.upsertSql(),
                task.id(), task.planId(), task.instruction(), task.status(), task.result(),
                Timestamp.from(task.createdAt()));
    }

    @Override
    public Optional<AgentDispatchTask> findById(String id) {
        List<AgentDispatchTask> found =
                jdbcTemplate.query("SELECT * FROM ddd4j_ai_agent_dispatch_task WHERE id = ?", ROW_MAPPER, id);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    @Override
    public List<AgentDispatchTask> findByPlanId(String planId) {
        return jdbcTemplate.query(
                "SELECT * FROM ddd4j_ai_agent_dispatch_task WHERE plan_id = ? ORDER BY created_at ASC",
                ROW_MAPPER, planId);
    }

    @Override
    public void update(AgentDispatchTask task) {
        // 与 save 同为"按 id 幂等写"：upsert 使重试与乱序调用都不会丢数据，
        // 且与 InMemoryAgentDispatchTaskRepository 的 put 行为一致（契约测试已锚定）。
        save(task);
    }
}
