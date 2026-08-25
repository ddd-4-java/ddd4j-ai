package io.ddd4j.ai.cmpt.asr.autoconfigure;

import io.ddd4j.ai.cmpt.asr.service.AsrService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AsrAutoConfiguration} 装配测试：默认装配 / 关闭回退。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class AsrAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AsrAutoConfiguration.class));

    @Test
    void defaultContext_registersAsrService() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(AsrService.class));
    }

    @Test
    void backsOffWhenDisabled() {
        contextRunner.withPropertyValues("ddd4j.ai.asr.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(AsrService.class));
    }
}
