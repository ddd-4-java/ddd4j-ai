package io.ddd4j.ai.extension.agent.store.jdbc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * JDBC 方言：承载建表 / 建索引 / 主键 upsert 三处不可避免的方言差异，
 * 使 {@link JdbcAgentDispatchTaskRepository} 本身保持方言无关。
 *
 * <p>两处经真库实测才发现的方言事实（H2 单测无法证伪）：
 * <ul>
 *   <li><b>MySQL 不支持 {@code CLOB}</b>（那是 Oracle/DB2/标准 SQL 的类型），
 *       大文本要用 {@code TEXT} / {@code MEDIUMTEXT} / {@code LONGTEXT}；
 *       此处取 {@code MEDIUMTEXT}（16MB）以留出余量。</li>
 *   <li><b>MySQL 不支持 {@code CREATE INDEX IF NOT EXISTS}</b>（那是 MariaDB 语法），
 *       索引必须写成建表语句内的 {@code KEY (...) }；PostgreSQL / H2 则支持独立语句。</li>
 * </ul>
 * 因此 DDL 以「语句列表」暴露：MySQL 一条（内联索引），PostgreSQL / H2 两条。
 *
 * <p>探测失败或未知产品名时回落到 H2 兼容写法（{@code MERGE INTO}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public enum JdbcDialect {

    MYSQL,
    POSTGRESQL,
    H2;

    private static final String TABLE = "ddd4j_ai_agent_dispatch_task";
    private static final String INDEX = "idx_ddd4j_ai_dispatch_plan";

    /** MySQL：大文本用 MEDIUMTEXT（16MB）；其余用 TEXT（PostgreSQL/H2 的 TEXT 无实际上限）。 */
    public String textType() {
        return this == MYSQL ? "MEDIUMTEXT" : "TEXT";
    }

    /**
     * 建表所需的全部 DDL 语句（按顺序执行）。
     *
     * <p>MySQL 因不支持 {@code CREATE INDEX IF NOT EXISTS} 而把索引内联在建表语句里，
     * 故只有一条语句；PostgreSQL / H2 为两条。
     */
    public List<String> ddlStatements() {
        String columns = "id VARCHAR(64) NOT NULL, "
                + "plan_id VARCHAR(64) NOT NULL, "
                + "instruction " + textType() + " NOT NULL, "
                + "status VARCHAR(16) NOT NULL, "
                + "result " + textType() + ", "
                + "created_at TIMESTAMP NOT NULL, "
                + "PRIMARY KEY (id)";
        if (this == MYSQL) {
            return List.of("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                    + columns + ", KEY " + INDEX + " (plan_id))");
        }
        return List.of(
                "CREATE TABLE IF NOT EXISTS " + TABLE + " (" + columns + ")",
                "CREATE INDEX IF NOT EXISTS " + INDEX + " ON " + TABLE + " (plan_id)");
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
            case MYSQL -> "INSERT INTO " + TABLE + " " + columns + " VALUES " + values
                    + " ON DUPLICATE KEY UPDATE plan_id=VALUES(plan_id), instruction=VALUES(instruction),"
                    + " status=VALUES(status), result=VALUES(result), created_at=VALUES(created_at)";
            case POSTGRESQL -> "INSERT INTO " + TABLE + " " + columns + " VALUES " + values
                    + " ON CONFLICT (id) DO UPDATE SET plan_id=EXCLUDED.plan_id,"
                    + " instruction=EXCLUDED.instruction, status=EXCLUDED.status,"
                    + " result=EXCLUDED.result, created_at=EXCLUDED.created_at";
            case H2 -> "MERGE INTO " + TABLE + " " + columns + " KEY(id) VALUES " + values;
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
