package io.ddd4j.ai.extension.memory;

import io.ddd4j.ai.extension.memory.service.MemoryService;
import io.ddd4j.ai.extension.memory.service.impl.WindowMemoryService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.RedisClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会话记忆 Redis 容器集成测试（镜像 redis:7，无专用 Testcontainers 模块，用 GenericContainer）：
 * 官方 RedisChatMemoryRepository 作为存储后端，验证 WindowMemoryService 持久化往返。无 Docker 自动跳过。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisChatMemoryIntegrationTest {

    // 官方 RedisChatMemoryRepository 需要 RediSearch（FT.CREATE 建索引），普通 redis 镜像不含该模块
    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis/redis-stack-server:7.4.0-v0"))
            .withExposedPorts(6379);

    static MemoryService memory;

    @BeforeAll
    static void setUp() {
        RedisClient client = RedisClient.create(REDIS.getHost(), REDIS.getMappedPort(6379));
        RedisChatMemoryRepository repository = RedisChatMemoryRepository.builder()
                .jedisClient(client)
                .build();
        memory = new WindowMemoryService(repository, MemoryService.DEFAULT_WINDOW_SIZE);
    }

    @Test
    void roundTripPersistsAcrossRedis() {
        memory.add("redis-conv", List.of(new UserMessage("第一问"), new AssistantMessage("第一答")));

        List<org.springframework.ai.chat.messages.Message> history = memory.get("redis-conv");

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getText()).isEqualTo("第一问");
        assertThat(history.get(1).getText()).isEqualTo("第一答");
    }

    @Test
    void clearRemovesRedisConversation() {
        memory.add("redis-clear", List.of(new UserMessage("临时")));
        memory.clear("redis-clear");

        assertThat(memory.get("redis-clear")).isEmpty();
    }
}
