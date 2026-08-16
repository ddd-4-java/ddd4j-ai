package io.ddd4j.ai.cmpt.rag;

import io.ddd4j.ai.cmpt.chat.service.impl.ChatClientAdapter;
import io.ddd4j.ai.cmpt.memory.service.MemoryService;
import io.ddd4j.ai.cmpt.memory.service.impl.WindowMemoryService;
import io.ddd4j.ai.cmpt.rag.properties.RagProperties;
import io.ddd4j.ai.cmpt.rag.service.RagService;
import io.ddd4j.ai.cmpt.rag.service.Reranker;
import io.ddd4j.ai.cmpt.rag.service.impl.RagPipeline;
import io.ddd4j.ai.cmpt.vectordb.service.VectorDbService;
import io.ddd4j.ai.cmpt.vectordb.service.impl.VectorStoreAdapter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * RAG 全链路联合冒烟（镜像：ollama/ollama + pgvector/pgvector:pg16）：
 * 摄取知识文档（all-minilm 嵌入 + pgvector 入库）→ 检索 → qwen2.5:0.5b 增强生成。
 * 无 Docker 或模型拉取失败时自动跳过。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Testcontainers(disabledWithoutDocker = true)
class RagSmokeIntegrationTest {

    private static final String CHAT_MODEL = "qwen2.5:0.5b";

    private static final String EMBEDDING_MODEL = "all-minilm";

    @Container
    static final OllamaContainer OLLAMA = new OllamaContainer(DockerImageName.parse("ollama/ollama:latest"));

    @Container
    static final PostgreSQLContainer<?> PGVECTOR = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    static RagService rag;

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
        assumeTrue(retryApiPresent,
                "类路径缺少 FW7 的 org.springframework.core.retry（父链为 FW 6.2），跳过 RAG 联合冒烟");

        // 容器内预拉嵌入与对话模型；网络受限时整组测试跳过
        var pullEmbedding = OLLAMA.execInContainer("ollama", "pull", EMBEDDING_MODEL);
        assumeTrue(pullEmbedding.getExitCode() == 0,
                "ollama pull " + EMBEDDING_MODEL + " 失败，跳过 RAG 联合冒烟");
        var pullChat = OLLAMA.execInContainer("ollama", "pull", CHAT_MODEL);
        assumeTrue(pullChat.getExitCode() == 0,
                "ollama pull " + CHAT_MODEL + " 失败，跳过 RAG 联合冒烟");

        OllamaApi api = OllamaApi.builder().baseUrl(OLLAMA.getEndpoint()).build();
        OllamaEmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
                .ollamaApi(api)
                .options(OllamaEmbeddingOptions.builder().model(EMBEDDING_MODEL).build())
                .build();
        OllamaChatModel chatModel = OllamaChatModel.builder()
                .ollamaApi(api)
                .options(OllamaChatOptions.builder().model(CHAT_MODEL).build())
                .build();

        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriver(DriverManager.getDriver(PGVECTOR.getJdbcUrl()));
        dataSource.setUrl(PGVECTOR.getJdbcUrl());
        dataSource.setUsername(PGVECTOR.getUsername());
        dataSource.setPassword(PGVECTOR.getPassword());
        PgVectorStore store = PgVectorStore.builder(new JdbcTemplate(dataSource), embeddingModel)
                .dimensions(384)
                .initializeSchema(true)
                .build();
        VectorDbService vectorDb = new VectorStoreAdapter(store, 4);

        MemoryService memory = new WindowMemoryService(new InMemoryChatMemoryRepository(), 20);
        ChatClientAdapter chat = new ChatClientAdapter(
                ChatClient.builder(chatModel).build(), memory, null);

        rag = new RagPipeline(chat, vectorDb, Reranker.NOOP, new RagProperties());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void ingestThenQueryAnswersFromKnowledgeBase() {
        rag.ingest(List.of(new Document("""
                灵犀计划是 PartMe.AI 团队 2026 年启动的内部代号，目标是让 ddd4j 框架家族
                在金融级业务系统中达到人类架构师的产出水准。灵犀计划分为三个阶段：
                契约定型、能力补齐、生态扩展。
                """, Map.of("source", "internal-wiki"))));

        String answer = rag.query("灵犀计划是什么？");

        assertThat(answer).isNotBlank();
        assertTrue(answer.contains("灵犀") || answer.contains("PartMe"),
                "回答应引用知识库内容，实际回答：" + answer);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void queryStreamEmitsNonEmptyAnswer() {
        var chunks = rag.queryStream("灵犀计划分几个阶段？").collectList().block();

        assertThat(chunks).isNotNull();
        assertThat(String.join("", chunks)).isNotBlank();
    }
}
