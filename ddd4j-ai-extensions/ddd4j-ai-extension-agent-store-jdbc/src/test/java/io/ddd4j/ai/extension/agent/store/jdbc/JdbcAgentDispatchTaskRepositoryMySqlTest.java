package io.ddd4j.ai.extension.agent.store.jdbc;

import io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepository;
import io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepositoryContract;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * MySQL 容器契约测试：与 H2 主路径跑**同一套断言**，验证跨库语义一致。
 *
 * <p>类名必须以 {@code Test} 结尾——本仓 surefire 无 failsafe，叫 {@code *IT} 会静默不运行。
 * 无 Docker 时按 Testcontainers 语义跳过。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Testcontainers
class JdbcAgentDispatchTaskRepositoryMySqlTest extends AgentDispatchTaskRepositoryContract {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @Override
    protected AgentDispatchTaskRepository newRepository() {
        return new JdbcAgentDispatchTaskRepository(
                new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()),
                JdbcDialect.MYSQL, true);
    }
}
