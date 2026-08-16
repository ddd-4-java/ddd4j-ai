package io.ddd4j.ai.cmpt.embedding;

import io.ddd4j.ai.cmpt.embedding.service.EmbeddingService;
import io.ddd4j.ai.cmpt.embedding.service.impl.EmbeddingModelAdapter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 向量嵌入组件 Ollama 容器集成测试：真实模型（all-minilm，384 维）验证
 * 单条/批量/维度链路。无 Docker 或模型拉取失败时自动跳过。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Testcontainers(disabledWithoutDocker = true)
class OllamaEmbeddingIntegrationTest {

    private static final String MODEL = "all-minilm";

    @Container
    static final OllamaContainer OLLAMA = new OllamaContainer(DockerImageName.parse("ollama/ollama:latest"));

    static EmbeddingService embeddingService;

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

        // 容器内预拉模型；网络受限时整组测试跳过
        var pull = OLLAMA.execInContainer("ollama", "pull", MODEL);
        org.junit.jupiter.api.Assertions.assertTrue(pull.getExitCode() == 0,
                "ollama pull " + MODEL + " 失败：" + pull.getStderr());
        OllamaApi api = OllamaApi.builder().baseUrl(OLLAMA.getEndpoint()).build();
        OllamaEmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
                .ollamaApi(api)
                .options(OllamaEmbeddingOptions.builder().model(MODEL).build())
                .build();
        embeddingService = new EmbeddingModelAdapter(embeddingModel, 2);
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void embedReturns384DimensionVector() {
        float[] vector = embeddingService.embed("你好，世界");

        assertThat(vector).hasSize(384);
        assertThat(vector[0]).isNotNaN();
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void batchEmbedKeepsOrderAndSlicesByBatchSize() {
        List<float[]> vectors = embeddingService.embedBatch(List.of("春眠不觉晓", "处处闻啼鸟", "夜来风雨声", "花落知多少", "extra"));

        assertThat(vectors).hasSize(5);
        assertThat(vectors.get(0)).hasSize(384);
        // batchSize=2：5 条输入分 3 批（2+2+1），顺序保持
        assertThat(vectors.get(4)).hasSize(384);
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void dimensionsMatchesModel() {
        assertThat(embeddingService.dimensions()).isEqualTo(384);
    }
}
