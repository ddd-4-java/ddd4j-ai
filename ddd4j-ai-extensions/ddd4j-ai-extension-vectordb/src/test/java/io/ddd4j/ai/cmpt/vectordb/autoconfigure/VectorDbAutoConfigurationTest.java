package io.ddd4j.ai.cmpt.vectordb.autoconfigure;

import io.ddd4j.ai.cmpt.vectordb.service.VectorDbService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link VectorDbAutoConfiguration} 装配测试。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class VectorDbAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VectorDbAutoConfiguration.class))
            .withBean("vectorStore", VectorStore.class, () -> mock(VectorStore.class));

    @Test
    void registersVectorDbServiceWhenStorePresent() {
        runner.run(context -> assertThat(context).hasSingleBean(VectorDbService.class));
    }

    @Test
    void backsOffWhenDisabled() {
        runner.withPropertyValues("ddd4j.ai.vectordb.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(VectorDbService.class));
    }

    @Test
    void backsOffWithoutVectorStore() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(VectorDbAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(VectorDbService.class));
    }
}
