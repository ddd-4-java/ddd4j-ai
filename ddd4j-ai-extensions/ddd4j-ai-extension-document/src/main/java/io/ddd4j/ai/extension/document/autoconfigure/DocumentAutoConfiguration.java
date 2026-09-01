package io.ddd4j.ai.extension.document.autoconfigure;

import io.ddd4j.ai.extension.asr.service.AsrService;
import io.ddd4j.ai.extension.document.DocumentParser;
import io.ddd4j.ai.extension.document.DocumentReader;
import io.ddd4j.ai.extension.document.parser.EasydocDocumentParser;
import io.ddd4j.ai.extension.document.parser.EasyexcelDocumentParser;
import io.ddd4j.ai.extension.document.parser.EasyodfDocumentParser;
import io.ddd4j.ai.extension.document.parser.EasypdfDocumentParser;
import io.ddd4j.ai.extension.document.parser.TikaDocumentParser;
import io.ddd4j.ai.extension.document.properties.DocumentProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 文档读取自动装配：注册统一门面 + 5 个解析器
 * （4 个高质量委托 + Tika 通用兜底），高优先级解析器未就位时门面自动降级。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@AutoConfiguration
@ConditionalOnProperty(name = "ddd4j.ai.document.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DocumentProperties.class)
public class DocumentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DocumentReader documentReader(List<DocumentParser> parsers) {
        return new DocumentReader(parsers);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "ddd4j.ai.document.enable-tika-fallback", havingValue = "true", matchIfMissing = true)
    public TikaDocumentParser tikaDocumentParser(DocumentProperties properties,
                                                 ObjectProvider<AsrService> asrService) {
        return new TikaDocumentParser(properties, asrService.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(name = "easypdfDocumentParser")
    @ConditionalOnProperty(name = "ddd4j.ai.document.enable-high-quality", havingValue = "true", matchIfMissing = true)
    public EasypdfDocumentParser easypdfDocumentParser() {
        return new EasypdfDocumentParser();
    }

    @Bean
    @ConditionalOnMissingBean(name = "easydocDocumentParser")
    @ConditionalOnProperty(name = "ddd4j.ai.document.enable-high-quality", havingValue = "true", matchIfMissing = true)
    public EasydocDocumentParser easydocDocumentParser() {
        return new EasydocDocumentParser();
    }

    @Bean
    @ConditionalOnMissingBean(name = "easyexcelDocumentParser")
    @ConditionalOnProperty(name = "ddd4j.ai.document.enable-high-quality", havingValue = "true", matchIfMissing = true)
    public EasyexcelDocumentParser easyexcelDocumentParser() {
        return new EasyexcelDocumentParser();
    }

    @Bean
    @ConditionalOnMissingBean(name = "easyodfDocumentParser")
    @ConditionalOnProperty(name = "ddd4j.ai.document.enable-high-quality", havingValue = "true", matchIfMissing = true)
    public EasyodfDocumentParser easyodfDocumentParser() {
        return new EasyodfDocumentParser();
    }
}
