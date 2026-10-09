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
 * @author ddd4j
 */
@AutoConfiguration
@ConditionalOnClass(JdbcTemplate.class)
@ConditionalOnProperty(name = "ddd4j.ai.agent.dispatch.repository", havingValue = "jdbc")
@ConditionalOnBean(DataSource.class)
public class JdbcDispatchRepositoryAutoConfiguration {

    /**
     * 默认构造器（自动装配类无状态，由 Spring 容器管理）。
     */
    public JdbcDispatchRepositoryAutoConfiguration() {
    }

    /**
     * 注册 JDBC 派发任务仓储：方言由实际 DataSource 推断，
     * 是否按建表语句自动初始化由 {@code ddd4j.ai.agent.dispatch.auto-ddl} 控制（默认 {@code true}）。
     *
     * @param dataSource   数据源
     * @param environment  环境配置，用于读取 {@code ddd4j.ai.agent.dispatch.auto-ddl}
     * @return 派发任务仓储实例
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
