package io.ddd4j.ai.cmpt.tts.autoconfigure;

import io.ddd4j.ai.cmpt.tts.service.TtsService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TtsAutoConfiguration} 装配测试：默认装配 / 关闭回退。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class TtsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TtsAutoConfiguration.class));

    @Test
    void defaultContext_registersTtsService() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(TtsService.class));
    }

    @Test
    void backsOffWhenDisabled() {
        contextRunner.withPropertyValues("ddd4j.ai.tts.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(TtsService.class));
    }
}
