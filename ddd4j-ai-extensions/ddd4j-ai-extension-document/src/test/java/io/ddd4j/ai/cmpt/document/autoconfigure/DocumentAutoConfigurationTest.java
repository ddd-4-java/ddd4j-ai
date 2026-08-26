package io.ddd4j.ai.cmpt.document.autoconfigure;

import io.ddd4j.ai.cmpt.document.DocumentReader;
import io.ddd4j.ai.cmpt.document.parser.EasydocDocumentParser;
import io.ddd4j.ai.cmpt.document.parser.EasyexcelDocumentParser;
import io.ddd4j.ai.cmpt.document.parser.EasyodfDocumentParser;
import io.ddd4j.ai.cmpt.document.parser.EasypdfDocumentParser;
import io.ddd4j.ai.cmpt.document.parser.TikaDocumentParser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DocumentAutoConfiguration} 装配测试：默认装配 / 关闭回退 / 特性开关。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class DocumentAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DocumentAutoConfiguration.class));

    @Test
    void defaultContext_registersReaderAndAllParsers() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DocumentReader.class);
            assertThat(context).hasSingleBean(TikaDocumentParser.class);
            assertThat(context).hasSingleBean(EasypdfDocumentParser.class);
            assertThat(context).hasSingleBean(EasydocDocumentParser.class);
            assertThat(context).hasSingleBean(EasyexcelDocumentParser.class);
            assertThat(context).hasSingleBean(EasyodfDocumentParser.class);
        });
    }

    @Test
    void backsOffWhenDisabled() {
        contextRunner.withPropertyValues("ddd4j.ai.document.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(DocumentReader.class));
    }

    @Test
    void highQualityDisabled_skipsDelegationParsers() {
        contextRunner.withPropertyValues("ddd4j.ai.document.enable-high-quality=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(DocumentReader.class);
                    assertThat(context).doesNotHaveBean(EasypdfDocumentParser.class);
                    assertThat(context).doesNotHaveBean(EasydocDocumentParser.class);
                    assertThat(context).doesNotHaveBean(EasyexcelDocumentParser.class);
                    assertThat(context).doesNotHaveBean(EasyodfDocumentParser.class);
                    assertThat(context).hasSingleBean(TikaDocumentParser.class);
                });
    }

    @Test
    void tikaFallbackDisabled_skipsFallbackParser() {
        contextRunner.withPropertyValues("ddd4j.ai.document.enable-tika-fallback=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(DocumentReader.class);
                    assertThat(context).doesNotHaveBean(TikaDocumentParser.class);
                });
    }
}
