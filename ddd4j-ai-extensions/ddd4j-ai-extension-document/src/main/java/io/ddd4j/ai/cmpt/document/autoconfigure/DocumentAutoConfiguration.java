package io.ddd4j.ai.cmpt.document.autoconfigure;

import io.ddd4j.ai.cmpt.document.DocumentParser;
import io.ddd4j.ai.cmpt.document.DocumentReader;
import io.ddd4j.ai.cmpt.document.parser.EasydocDocumentParser;
import io.ddd4j.ai.cmpt.document.parser.EasyexcelDocumentParser;
import io.ddd4j.ai.cmpt.document.parser.EasyodfDocumentParser;
import io.ddd4j.ai.cmpt.document.parser.EasypdfDocumentParser;
import io.ddd4j.ai.cmpt.document.parser.TikaDocumentParser;
import io.ddd4j.ai.cmpt.document.properties.DocumentProperties;
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
    public TikaDocumentParser tikaDocumentParser(DocumentProperties properties) {
        return new TikaDocumentParser(properties);
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
