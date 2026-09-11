package io.ddd4j.ai.extension.agent.store.jdbc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JdbcDialect} 方言测试：锚定"一处覆盖三库"所依赖的三处方言差异
 * （大文本列类型 / 建索引 / 主键 upsert）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class JdbcDialectTest {

    @Test
    void mysqlUsesClobAndOnDuplicateKey() {
        JdbcDialect dialect = JdbcDialect.forProductName("MySQL");
        assertThat(dialect.createTableSql()).contains("CLOB");
        assertThat(dialect.upsertSql()).contains("ON DUPLICATE KEY UPDATE");
    }

    @Test
    void postgresUsesTextAndOnConflict() {
        JdbcDialect dialect = JdbcDialect.forProductName("PostgreSQL");
        assertThat(dialect.createTableSql()).contains("TEXT").doesNotContain("CLOB");
        assertThat(dialect.upsertSql()).contains("ON CONFLICT").contains("DO UPDATE");
    }

    @Test
    void h2UsesTextAndMerge() {
        JdbcDialect dialect = JdbcDialect.forProductName("H2");
        assertThat(dialect.createTableSql()).contains("TEXT").doesNotContain("CLOB");
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
}
