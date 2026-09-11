package io.ddd4j.ai.extension.agent.store.jdbc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * JDBC 方言：承载建表 / 建索引 / 主键 upsert 三处不可避免的方言差异，
 * 使 {@link JdbcAgentDispatchTaskRepository} 本身保持方言无关。
 *
 * <p>探测失败或未知产品名时回落到 H2 兼容写法（{@code MERGE INTO}）——
 * 它是 SQL 标准中最接近"按主键 upsert"的表述。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public enum JdbcDialect {

    MYSQL,
    POSTGRESQL,
    H2;

    /** 大文本列类型：MySQL 的 {@code TEXT} 上限较小，用 {@code CLOB}；其余用 {@code TEXT}。 */
    public String textType() {
        return this == MYSQL ? "CLOB" : "TEXT";
    }

    public String createTableSql() {
        return "CREATE TABLE IF NOT EXISTS ddd4j_ai_agent_dispatch_task ("
                + "id VARCHAR(64) NOT NULL, "
                + "plan_id VARCHAR(64) NOT NULL, "
                + "instruction " + textType() + " NOT NULL, "
                + "status VARCHAR(16) NOT NULL, "
                + "result " + textType() + ", "
                + "created_at TIMESTAMP NOT NULL, "
                + "PRIMARY KEY (id))";
    }

    public String createIndexSql() {
        return "CREATE INDEX IF NOT EXISTS idx_ddd4j_ai_dispatch_plan "
                + "ON ddd4j_ai_agent_dispatch_task (plan_id)";
    }

    /**
     * 按主键幂等写入（upsert）。
     *
     * <p>参数顺序固定为：{@code id, plan_id, instruction, status, result, created_at}。
     */
    public String upsertSql() {
        String columns = "(id, plan_id, instruction, status, result, created_at)";
        String values = "(?, ?, ?, ?, ?, ?)";
        return switch (this) {
            case MYSQL -> "INSERT INTO ddd4j_ai_agent_dispatch_task " + columns + " VALUES " + values
                    + " ON DUPLICATE KEY UPDATE plan_id=VALUES(plan_id), instruction=VALUES(instruction),"
                    + " status=VALUES(status), result=VALUES(result), created_at=VALUES(created_at)";
            case POSTGRESQL -> "INSERT INTO ddd4j_ai_agent_dispatch_task " + columns + " VALUES " + values
                    + " ON CONFLICT (id) DO UPDATE SET plan_id=EXCLUDED.plan_id,"
                    + " instruction=EXCLUDED.instruction, status=EXCLUDED.status,"
                    + " result=EXCLUDED.result, created_at=EXCLUDED.created_at";
            case H2 -> "MERGE INTO ddd4j_ai_agent_dispatch_task " + columns + " KEY(id) VALUES " + values;
        };
    }

    /** 按数据库产品名选择方言；未知或 null 回落 H2 兼容写法。 */
    public static JdbcDialect forProductName(String productName) {
        if (productName == null) {
            return H2;
        }
        String normalized = productName.toLowerCase();
        if (normalized.contains("mysql") || normalized.contains("mariadb")) {
            return MYSQL;
        }
        if (normalized.contains("postgres")) {
            return POSTGRESQL;
        }
        return H2;
    }

    /** 从连接元数据探测方言。 */
    public static JdbcDialect from(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return forProductName(connection.getMetaData().getDatabaseProductName());
        } catch (SQLException e) {
            throw new IllegalStateException("无法探测数据库方言: " + e.getMessage(), e);
        }
    }
}
