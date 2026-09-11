package io.ddd4j.ai.extension.tts.autoconfigure;

import io.ddd4j.ai.extension.tts.metrics.TtsMetrics;
import io.ddd4j.ai.extension.tts.router.TtsRouter;
import io.ddd4j.ai.extension.tts.service.TtsService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TtsAutoConfiguration} 装配测试：默认装配 / 关闭回退 / primary 切换。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class TtsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TtsAutoConfiguration.class));

    @Test
    void defaultContext_registersRouterMetricsAndEdgeService() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TtsRouter.class);
            assertThat(context).hasSingleBean(TtsMetrics.class);
            assertThat(context).hasBean("edgeTtsService");
            // DashScope 后端无 api-key 不会装配
            assertThat(context).doesNotHaveBean("dashScopeRealtimeTtsService");
            // primary=edge 时链路应只有 edge
            TtsRouter router = context.getBean(TtsRouter.class);
            assertThat(router.backendNames()).containsExactly("edgeTtsService");
        });
    }

    @Test
    void backsOffWhenDisabled() {
        contextRunner.withPropertyValues("ddd4j.ai.tts.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(TtsRouter.class);
                    assertThat(context).doesNotHaveBean(TtsService.class);
                    assertThat(context).doesNotHaveBean(TtsMetrics.class);
                });
    }

    @Test
    void withDashScopeApiKey_registersBothBackends() {
        contextRunner
                .withPropertyValues(
                        "ddd4j.ai.tts.primary=dashscope",
                        "ddd4j.ai.tts.dashscope.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasBean("dashScopeRealtimeTtsService");
                    assertThat(context).hasBean("edgeTtsService");
                    TtsRouter router = context.getBean(TtsRouter.class);
                    // primary=dashscope → dashscope 在前，edge 在后
                    assertThat(router.backendNames())
                            .containsExactly("dashScopeRealtimeTtsService", "edgeTtsService");
                });
    }

    @Test
    void withoutDashScopeApiKey_fallsBackToEdgeOnly() {
        contextRunner
                .withPropertyValues("ddd4j.ai.tts.primary=dashscope")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("dashScopeRealtimeTtsService");
                    TtsRouter router = context.getBean(TtsRouter.class);
                    assertThat(router.backendNames()).containsExactly("edgeTtsService");
                });
    }

    @Test
    void edgeDisabled_dashScopeOnly() {
        contextRunner
                .withPropertyValues(
                        "ddd4j.ai.tts.edge.enabled=false",
                        "ddd4j.ai.tts.dashscope.api-key=test-key")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("edgeTtsService");
                    assertThat(context).hasBean("dashScopeRealtimeTtsService");
                    TtsRouter router = context.getBean(TtsRouter.class);
                    assertThat(router.backendNames()).containsExactly("dashScopeRealtimeTtsService");
                });
    }

    @Test
    void metricsDisabled_omitsMetricsBean() {
        contextRunner
                .withPropertyValues("ddd4j.ai.tts.metrics.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(TtsRouter.class);
                    assertThat(context).doesNotHaveBean(TtsMetrics.class);
                });
    }

    @Test
    void primaryInvalidFallsBackToEdge() {
        contextRunner
                .withPropertyValues("ddd4j.ai.tts.primary=unknown-vendor")
                .run(context -> {
                    TtsRouter router = context.getBean(TtsRouter.class);
                    assertThat(router.backendNames()).containsExactly("edgeTtsService");
                });
    }
}