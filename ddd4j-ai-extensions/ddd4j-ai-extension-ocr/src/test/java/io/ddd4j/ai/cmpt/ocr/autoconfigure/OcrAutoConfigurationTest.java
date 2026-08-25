package io.ddd4j.ai.cmpt.ocr.autoconfigure;

import io.ddd4j.ai.cmpt.ocr.service.OcrService;
import io.ddd4j.ai.cmpt.ocr.service.impl.CompositeOcrService;
import io.ddd4j.ai.cmpt.ocr.service.impl.PdfBoxOcrService;
import io.ddd4j.ai.cmpt.ocr.service.impl.TikaOcrService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OcrAutoConfiguration} 装配测试：默认装配 / 关闭回退 / 自定义端口覆盖。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class OcrAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OcrAutoConfiguration.class));

    @Test
    void defaultContext_registersStrategyAndPort() {
        contextRunner.run(context -> {
            assertThat(context.getBeansOfType(OcrService.class)).hasSize(3);
            assertThat(context.getBean(OcrService.class)).isInstanceOf(CompositeOcrService.class);
            assertThat(context).hasSingleBean(PdfBoxOcrService.class);
            assertThat(context).hasSingleBean(TikaOcrService.class);
        });
    }

    @Test
    void backsOffWhenDisabled() {
        contextRunner.withPropertyValues("ddd4j.ai.ocr.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(OcrService.class));
    }

    @Test
    void respectsCustomOcrService() {
        contextRunner.withUserConfiguration(CustomOcrServiceConfiguration.class)
                .run(context -> {
                    assertThat(context).hasBean("customOcrService");
                    assertThat(context.getBean("customOcrService")).isInstanceOf(CustomOcrService.class);
                    assertThat(context).doesNotHaveBean(CompositeOcrService.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomOcrServiceConfiguration {

        @Bean
        OcrService customOcrService() {
            return new CustomOcrService();
        }
    }

    static class CustomOcrService implements OcrService {

        @Override
        public String extractText(InputStream input, MediaType mediaType) {
            return "custom";
        }

        @Override
        public List<Document> extract(InputStream input, MediaType mediaType) {
            return List.of();
        }
    }
}
