package io.ddd4j.ai.extension.agent.agent;

import io.agentscope.core.message.AssistantMessage;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.extensions.mysql.state.MysqlAgentStateStore;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * mysql 状态持久化接线测试：真实 MySQL 容器验证 {@link MysqlAgentStateStore}
 * 保存/读取 roundtrip（autoCreate 建库建表）。无 Docker 时优雅跳过。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Testcontainers(disabledWithoutDocker = true)
class MysqlStateStoreWiringTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @Test
    void mysqlStateStore_roundtrip() {
        AgentStateStore store = new MysqlAgentStateStore(dataSource(), true);

        store.save("agent-1", "session-1", "last-reply", new AssistantMessage("roundtrip-ok"));

        var loaded = store.get("agent-1", "session-1", "last-reply", AssistantMessage.class);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getTextContent()).isEqualTo("roundtrip-ok");
    }

    @Test
    void mysqlStateStore_existsAndDelete() {
        AgentStateStore store = new MysqlAgentStateStore(dataSource(), true);
        String session = "s-" + UUID.randomUUID();
        store.save("agent-1", session, "k", new AssistantMessage("v"));

        assertThat(store.exists("agent-1", session)).isTrue();
        store.delete("agent-1", session);
        assertThat(store.exists("agent-1", session)).isFalse();
    }

    private static DataSource dataSource() {
        com.mysql.cj.jdbc.MysqlDataSource ds = new com.mysql.cj.jdbc.MysqlDataSource();
        ds.setUrl(MYSQL.getJdbcUrl());
        ds.setUser("root");
        ds.setPassword(MYSQL.getPassword());
        return ds;
    }
}
