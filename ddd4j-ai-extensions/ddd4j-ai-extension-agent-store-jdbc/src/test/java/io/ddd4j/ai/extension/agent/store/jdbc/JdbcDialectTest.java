package io.ddd4j.ai.extension.agent.store.jdbc;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JdbcDialect} 方言测试：锚定"一处覆盖三库"所依赖的方言差异
 * （大文本列类型 / 索引写法 / 主键 upsert）。
 *
 * <p><b>注意本测试的效力边界</b>：它只保证"代码与这里的期望一致"，**不保证与真库兼容**。
 * 本项开发中它曾与代码共享同一个错误假设（MySQL 用 CLOB）而全绿，最终由
 * MySQL 容器契约测试证伪。因此真库验证不可省——本测试是回归护栏，不是正确性证明。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class JdbcDialectTest {

    @Test
    void mysqlUsesMediumTextAndInlineIndex() {
        JdbcDialect dialect = JdbcDialect.MYSQL;
        List<String> statements = dialect.ddlStatements();

        // MySQL 不支持 CLOB，也不支持 CREATE INDEX IF NOT EXISTS
        assertThat(statements).hasSize(1);
        assertThat(statements.get(0))
                .contains("MEDIUMTEXT")
                .doesNotContain("CLOB")
                .contains("KEY idx_ddd4j_ai_dispatch_plan");
        assertThat(dialect.upsertSql()).contains("ON DUPLICATE KEY UPDATE");
    }

    @Test
    void postgresUsesTextAndSeparateIndex() {
        JdbcDialect dialect = JdbcDialect.POSTGRESQL;
        List<String> statements = dialect.ddlStatements();

        assertThat(statements).hasSize(2);
        assertThat(statements.get(0)).contains("TEXT").doesNotContain("CLOB").doesNotContain("KEY idx_");
        assertThat(statements.get(1)).contains("CREATE INDEX IF NOT EXISTS");
        assertThat(dialect.upsertSql()).contains("ON CONFLICT").contains("DO UPDATE");
    }

    @Test
    void h2UsesTextAndSeparateIndex() {
        JdbcDialect dialect = JdbcDialect.H2;
        List<String> statements = dialect.ddlStatements();

        assertThat(statements).hasSize(2);
        assertThat(statements.get(0)).contains("TEXT").doesNotContain("CLOB");
        assertThat(dialect.upsertSql()).contains("MERGE INTO");
    }

    @Test
    void unknownProductFallsBackToH2Compatible() {
        assertThat(JdbcDialect.forProductName("SomeUnknownDb").upsertSql()).contains("MERGE INTO");
    }

    @Test
    void nullProductFallsBackToH2Compatible() {
        assertThat(JdbcDialect.forProductName(null)).isEqualTo(JdbcDialect.H2);
    }

    @Test
    void mariadbProductNameIsTreatedAsMysql() {
        assertThat(JdbcDialect.forProductName("MariaDB")).isEqualTo(JdbcDialect.MYSQL);
    }
}
