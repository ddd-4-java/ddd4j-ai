package io.ddd4j.ai.sample;

import io.ddd4j.ai.cmpt.chat.service.ChatService;
import io.ddd4j.ai.cmpt.rag.service.RagService;
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
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "ddd4j.ai.rag.chunking=false")
class SampleAppOllamaSmokeTest {

    @Container
    static final OllamaContainer OLLAMA = new OllamaContainer(DockerImageName.parse("ollama/ollama:latest"));

    @DynamicPropertySource
    static void ollamaEndpoint(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.ollama.base-url", OLLAMA::getEndpoint);
    }

    @BeforeAll
    static void pullModels() throws Exception {
        assumeTrue(OLLAMA.execInContainer("ollama", "pull", "qwen2.5:0.5b").getExitCode() == 0,
                "ollama pull qwen2.5:0.5b 失败，跳过示例应用冒烟");
        assumeTrue(OLLAMA.execInContainer("ollama", "pull", "all-minilm").getExitCode() == 0,
                "ollama pull all-minilm 失败，跳过示例应用冒烟");
    }

    @Autowired
    ChatService chatService;

    @Autowired
    RagService ragService;

    @Autowired
    io.ddd4j.ai.cmpt.memory.service.MemoryService memoryService;

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
                "灵犀计划是 PartMe.AI 团队 2026 年启动的内部代号，目标是让 ddd4j 框架家族达到人类架构师的产出水准。",
                Map.of("source", "smoke"))));

        String answer = ragService.query("灵犀计划是什么？");

        assertThat(answer).isNotBlank();
        assertTrue(answer.contains("灵犀"), "RAG 回答应引用知识库，实际：" + answer);
    }
}
