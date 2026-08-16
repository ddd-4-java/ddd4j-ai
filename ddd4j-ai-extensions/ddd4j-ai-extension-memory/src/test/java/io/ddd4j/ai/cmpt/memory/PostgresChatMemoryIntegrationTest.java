package io.ddd4j.ai.cmpt.memory;

import io.ddd4j.ai.cmpt.memory.service.MemoryService;
import io.ddd4j.ai.cmpt.memory.service.impl.WindowMemoryService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会话记忆 PostgreSQL 容器集成测试：官方 JdbcChatMemoryRepository 作为存储后端，
 * 验证 WindowMemoryService 的持久化往返与窗口截断。无 Docker 自动跳过。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgresChatMemoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static MemoryService memory;

    @BeforeAll
    static void setUp() throws Exception {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriver(DriverManager.getDriver(POSTGRES.getJdbcUrl()));
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUsername(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        // 执行官方 PostgreSQL 建表脚本（随 spring-ai-model-chat-memory-repository-jdbc 发布）
        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(
                    "org/springframework/ai/chat/memory/repository/jdbc/schema-postgresql.sql"));
        }

        JdbcChatMemoryRepository repository = JdbcChatMemoryRepository.builder()
                .jdbcTemplate(jdbcTemplate)
                .build();
        memory = new WindowMemoryService(repository, MemoryService.DEFAULT_WINDOW_SIZE);
    }

    @Test
    void roundTripPersistsAcrossRepository() {
        memory.add("pg-conv", List.of(new UserMessage("你好"), new AssistantMessage("你好，有什么可以帮你？")));

        List<org.springframework.ai.chat.messages.Message> history = memory.get("pg-conv");

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getText()).isEqualTo("你好");
        assertThat(history.get(1).getText()).isEqualTo("你好，有什么可以帮你？");
    }

    @Test
    void clearRemovesPersistedConversation() {
        memory.add("pg-clear", List.of(new UserMessage("临时消息")));
        memory.clear("pg-clear");

        assertThat(memory.get("pg-clear")).isEmpty();
    }

    @Test
    void windowTrimsPersistedHistory() throws Exception {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriver(DriverManager.getDriver(POSTGRES.getJdbcUrl()));
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUsername(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        MemoryService small = new WindowMemoryService(
                JdbcChatMemoryRepository.builder().dataSource(dataSource).build(), 3);

        for (int i = 1; i <= 6; i++) {
            small.add("pg-window", List.of(new UserMessage("消息" + i)));
        }

        List<org.springframework.ai.chat.messages.Message> window = small.get("pg-window");
        assertThat(window).hasSize(3);
        assertThat(window.get(0).getText()).isEqualTo("消息4");
        assertThat(window.get(2).getText()).isEqualTo("消息6");
    }
}
