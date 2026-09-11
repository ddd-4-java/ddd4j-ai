package io.ddd4j.ai.extension.agent.store.jdbc.autoconfigure;

import io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepository;
import io.ddd4j.ai.extension.agent.store.jdbc.JdbcAgentDispatchTaskRepository;
import io.ddd4j.ai.extension.agent.store.jdbc.JdbcDialect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * JDBC 派发任务仓储自动装配：**仅在显式开启且存在 DataSource 时生效**。
 *
 * <p>不配置 {@code ddd4j.ai.agent.dispatch.repository} 时本类整体退让，
 * 由 agent 扩展既有的内存实现接管——既有用户零影响。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@AutoConfiguration
@ConditionalOnClass(JdbcTemplate.class)
@ConditionalOnProperty(name = "ddd4j.ai.agent.dispatch.repository", havingValue = "jdbc")
@ConditionalOnBean(DataSource.class)
public class JdbcDispatchRepositoryAutoConfiguration {

    /**
     * @param autoDdl {@code ddd4j.ai.agent.dispatch.auto-ddl}，默认 {@code true}
     */
    @Bean
    @ConditionalOnMissingBean(AgentDispatchTaskRepository.class)
    public AgentDispatchTaskRepository jdbcAgentDispatchTaskRepository(DataSource dataSource,
                                                                       Environment environment) {
        boolean autoDdl = environment.getProperty(
                "ddd4j.ai.agent.dispatch.auto-ddl", Boolean.class, Boolean.TRUE);
        return new JdbcAgentDispatchTaskRepository(dataSource, JdbcDialect.from(dataSource), autoDdl);
    }
}
