package io.ddd4j.ai.extension.agent.store.jdbc.autoconfigure;

import io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepository;
import io.ddd4j.ai.extension.agent.store.jdbc.JdbcAgentDispatchTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JdbcDispatchRepositoryAutoConfiguration} 装配分支测试。
 *
 * <p>覆盖 4 个分支：属性开+DataSource 有 / 属性缺 / DataSource 缺 / {@code auto-ddl=false}。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class JdbcDispatchRepositoryAutoConfigurationTest {

    @Configuration
    static class DataSourceConfig {
        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .generateUniqueName(true)
                    .build();
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JdbcDispatchRepositoryAutoConfiguration.class));

    @Test
    void jdbcPropertyWithDataSource_registersJdbcRepository() {
        runner.withUserConfiguration(DataSourceConfig.class)
                .withPropertyValues("ddd4j.ai.agent.dispatch.repository=jdbc")
                .run(context -> assertThat(context).hasSingleBean(JdbcAgentDispatchTaskRepository.class));
    }

    @Test
    void withoutProperty_backsOffEntirely() {
        runner.withUserConfiguration(DataSourceConfig.class)
                .run(context -> assertThat(context).doesNotHaveBean(AgentDispatchTaskRepository.class));
    }

    @Test
    void jdbcPropertyWithoutDataSource_backsOff() {
        runner.withPropertyValues("ddd4j.ai.agent.dispatch.repository=jdbc")
                .run(context -> assertThat(context).doesNotHaveBean(AgentDispatchTaskRepository.class));
    }

    @Test
    void autoDdlFalse_stillRegistersRepository() {
        runner.withUserConfiguration(DataSourceConfig.class)
                .withPropertyValues(
                        "ddd4j.ai.agent.dispatch.repository=jdbc",
                        "ddd4j.ai.agent.dispatch.auto-ddl=false")
                .run(context -> assertThat(context).hasSingleBean(JdbcAgentDispatchTaskRepository.class));
    }
}
