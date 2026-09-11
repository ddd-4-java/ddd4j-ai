package io.ddd4j.ai.extension.agent.store.jdbc;

import io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepository;
import io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepositoryContract;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

/**
 * H2 内存库上的契约测试——**主路径，不需要 Docker**，因此在任何环境都会真实执行。
 *
 * <p>容器化的 MySQL / PostgreSQL 契约测试只做跨库补充验证（见
 * {@code JdbcAgentDispatchTaskRepositoryMySqlTest} / {@code ...PostgresTest}）；
 * 若把核心断言只压在容器上，无 Docker 的机器会静默跳过，得到骗人的绿灯。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class JdbcAgentDispatchTaskRepositoryH2Test extends AgentDispatchTaskRepositoryContract {

    private static DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .build();
    }

    @Override
    protected AgentDispatchTaskRepository newRepository() {
        return new JdbcAgentDispatchTaskRepository(dataSource(), JdbcDialect.H2, true);
    }

    @Test
    void autoDdlFalse_doesNotCreateTable() {
        JdbcAgentDispatchTaskRepository repository =
                new JdbcAgentDispatchTaskRepository(dataSource(), JdbcDialect.H2, false);
        // 未建表时查询必须**失败**而非静默返回空——静默会把配置错误伪装成"没有任务"
        Assertions.assertThrows(RuntimeException.class, () -> repository.findByPlanId("p1"));
    }
}
