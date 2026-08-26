package io.ddd4j.ai.extension.embedding.autoconfigure;

import io.ddd4j.ai.extension.embedding.service.EmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link EmbeddingAutoConfiguration} 装配测试。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class EmbeddingAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EmbeddingAutoConfiguration.class))
            .withBean("embeddingModel", EmbeddingModel.class, () -> mock(EmbeddingModel.class));

    @Test
    void registersEmbeddingServiceWhenModelPresent() {
        runner.run(context -> assertThat(context).hasSingleBean(EmbeddingService.class));
    }

    @Test
    void backsOffWhenDisabled() {
        runner.withPropertyValues("ddd4j.ai.embedding.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(EmbeddingService.class));
    }

    @Test
    void backsOffWithoutEmbeddingModel() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EmbeddingAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(EmbeddingService.class));
    }
}
