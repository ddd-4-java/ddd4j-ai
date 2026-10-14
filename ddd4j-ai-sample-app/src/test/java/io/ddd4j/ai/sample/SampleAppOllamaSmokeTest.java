package io.ddd4j.ai.sample;

import io.ddd4j.ai.extension.chat.service.ChatService;
import io.ddd4j.ai.extension.rag.service.RagService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 示例应用全链路冒烟：Ollama 容器（qwen2.5:0.5b + all-minilm）驱动真实模型，
 * 验证「引依赖即装配」的开箱体验（chat 单轮/多轮 + RAG 摄取/问答）。
 * 无 Docker 或模型拉取失败时自动跳过。
 *
 * @author ddd4j
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "ddd4j.ai.rag.chunking=false")
class SampleAppOllamaSmokeTest {

    // 测试镜像来源: https://testcontainers.com/modules/ollama/
    // 镜像钉版 ollama/ollama:0.40.1（来源: https://hub.docker.com/v2/repositories/ollama/ollama/tags?page_size=25，
    // 2026-10-08 查询确认 0.40.1 为最新稳定版；钉死非 latest 标签消除浮动镜像导致的测试不可复现）
    @Container
    static final OllamaContainer OLLAMA = new OllamaContainer(DockerImageName.parse("ollama/ollama:0.40.1"));
    @Autowired
    ChatService chatService;
    @Autowired
    RagService ragService;
    @Autowired
    io.ddd4j.ai.extension.memory.service.MemoryService memoryService;

    @DynamicPropertySource
    static void ollamaEndpoint(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.ollama.base-url", OLLAMA::getEndpoint);
    }

    @BeforeAll
    static void pullModels() throws Exception {
        try {
            assumeTrue(OLLAMA.execInContainer("ollama", "pull", "qwen2.5:0.5b").getExitCode() == 0,
                    "ollama pull qwen2.5:0.5b 失败，跳过示例应用冒烟");
            assumeTrue(OLLAMA.execInContainer("ollama", "pull", "all-minilm").getExitCode() == 0,
                    "ollama pull all-minilm 失败，跳过示例应用冒烟");
        } catch (Exception e) {
            assumeTrue(false, "ollama pull 异常（视为网络受限跳过）：" + e);
        }
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void multiTurnChatKeepsSession() {
        String first = chatService.chat("我叫小明，请记住", "smoke-session");
        String second = chatService.chat("我叫什么名字？", "smoke-session");

        // 小模型（0.5b）的指令遵循不稳定，断言聚焦链路确定性：
        // 两轮均有回答，且会话记忆正确累积 4 条（2 轮 × user+assistant 写回）
        assertThat(first).isNotBlank();
        assertThat(second).isNotBlank();
        assertThat(memoryService.get("smoke-session")).hasSize(4);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void ragIngestThenQueryAnswersFromKnowledge() {
        ragService.ingest(List.of(new Document(
                "灵犀计划是 ddd4j 团队 2026 年启动的内部代号，目标是让 ddd4j 框架家族达到人类架构师的产出水准。",
                Map.of("source", "smoke"))));

        String answer = ragService.query("灵犀计划是什么？");

        assertThat(answer).isNotBlank();
        assertTrue(answer.contains("灵犀"), "RAG 回答应引用知识库，实际：" + answer);
    }
}
