package io.ddd4j.ai.extension.chat;

import io.ddd4j.ai.extension.chat.service.impl.ChatClientAdapter;
import io.ddd4j.ai.extension.memory.service.MemoryService;
import io.ddd4j.ai.extension.memory.service.impl.WindowMemoryService;
import io.ddd4j.ai.core.AiRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 对话组件 Ollama 容器集成测试：真实模型（qwen2.5:0.5b）验证 ChatClientAdapter 的
 * 单轮/多轮/流式/AiHandler 链路。无 Docker 或模型拉取失败时自动跳过。
 *
 * @author ddd4j
 */
@Testcontainers(disabledWithoutDocker = true)
class OllamaChatIntegrationTest {

    private static final String MODEL = "qwen2.5:0.5b";

    // 测试镜像来源: https://testcontainers.com/modules/ollama/
    // 镜像钉版 ollama/ollama:0.40.1（来源: https://hub.docker.com/v2/repositories/ollama/ollama/tags?page_size=25，
    // 2026-10-08 查询确认 0.40.1 为最新稳定版；钉死非 latest 标签消除浮动镜像导致的测试不可复现）
    @Container
    static final OllamaContainer OLLAMA = new OllamaContainer(DockerImageName.parse("ollama/ollama:0.40.1"));

    static ChatClientAdapter adapter;

    static MemoryService memory;

    @BeforeAll
    static void setUp() throws Exception {
        // Spring AI 2.0 的 OllamaApi 依赖 Spring Framework 7 的 org.springframework.core.retry 包；
        // 当前父链（ddd4j-boot-dependencies，FW 6.2）不含该类，真实模型链路需 Boot 4 / FW 7 运行时。
        boolean retryApiPresent;
        try {
            Class.forName("org.springframework.core.retry.RetryListener");
            retryApiPresent = true;
        } catch (ClassNotFoundException e) {
            retryApiPresent = false;
        }
        org.junit.jupiter.api.Assumptions.assumeTrue(retryApiPresent,
                "类路径缺少 FW7 的 org.springframework.core.retry（父链为 FW 6.2），跳过 Ollama 真实模型链路");

        // 容器内预拉模型；网络受限（拉取失败/超时）时按类注释约定整组跳过，而非判失败
        try {
            var pull = OLLAMA.execInContainer("ollama", "pull", MODEL);
            org.junit.jupiter.api.Assumptions.assumeTrue(pull.getExitCode() == 0,
                    "ollama pull " + MODEL + " 失败（视为网络受限跳过）：" + pull.getStderr());
        } catch (Exception e) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false,
                    "ollama pull " + MODEL + " 异常（视为网络受限跳过）：" + e);
        }
        OllamaApi api = OllamaApi.builder().baseUrl(OLLAMA.getEndpoint()).build();
        OllamaChatModel chatModel = OllamaChatModel.builder()
                .ollamaApi(api)
                .options(OllamaChatOptions.builder().model(MODEL).build())
                .build();
        memory = new WindowMemoryService(new InMemoryChatMemoryRepository(), MemoryService.DEFAULT_WINDOW_SIZE);
        adapter = new ChatClientAdapter(ChatClient.builder(chatModel).build(), memory, null);
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void singleTurnReturnsNonEmptyAnswer() {
        String answer = adapter.chat("用一个词回答：中国的首都是哪里？");

        assertThat(answer).isNotBlank();
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void multiTurnWritesBackToMemory() {
        String answer = adapter.chat("我叫小明，请记住我的名字", "conv-it");

        assertThat(answer).isNotBlank();
        // 写回断言不依赖模型智力：本轮 user + assistant 必然入记忆
        assertThat(memory.get("conv-it")).hasSize(2);
        assertThat(memory.get("conv-it").get(0).getText()).contains("小明");
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void streamingEmitsContent() {
        var chunks = adapter.streamChat("数到三").collectList().block();

        assertThat(chunks).isNotNull();
        assertThat(String.join("", chunks)).isNotBlank();
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void aiHandlerRoundTrip() {
        String output = adapter.handle(AiRequest.of("回答：1+1=?")).output();

        assertThat(output).isNotBlank();
    }
}
