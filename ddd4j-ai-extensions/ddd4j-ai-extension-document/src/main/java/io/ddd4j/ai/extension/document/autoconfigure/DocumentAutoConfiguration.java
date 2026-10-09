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
 * @author ddd4j
 */
@AutoConfiguration
@ConditionalOnProperty(name = "ddd4j.ai.document.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DocumentProperties.class)
public class DocumentAutoConfiguration {

    /**
     * 默认构造器（自动装配类无状态）。
     */
    public DocumentAutoConfiguration() {
    }

    /**
     * 装配统一文档读取门面（按解析器优先级路由并自动降级）。
     *
     * @param parsers 容器内全部解析器
     * @return 文档读取门面 Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public DocumentReader documentReader(List<DocumentParser> parsers) {
        return new DocumentReader(parsers);
    }

    /**
     * 装配 Tika 通用兜底解析器（可选挂接 OCR 服务）。
     *
     * @param properties 文档配置属性
     * @param asrService 语音服务提供器（此处未使用，保留扩展位）
     * @return Tika 解析器 Bean
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "ddd4j.ai.document.enable-tika-fallback", havingValue = "true", matchIfMissing = true)
    public TikaDocumentParser tikaDocumentParser(DocumentProperties properties,
                                                 ObjectProvider<AsrService> asrService) {
        return new TikaDocumentParser(properties, asrService.getIfAvailable());
    }

    /**
     * 装配 PDF 高质量委托解析器。
     *
     * @return PDF 解析器 Bean
     */
    @Bean
    @ConditionalOnMissingBean(name = "easypdfDocumentParser")
    @ConditionalOnProperty(name = "ddd4j.ai.document.enable-high-quality", havingValue = "true", matchIfMissing = true)
    public EasypdfDocumentParser easypdfDocumentParser() {
        return new EasypdfDocumentParser();
    }

    /**
     * 装配 Word 高质量委托解析器。
     *
     * @return Word 解析器 Bean
     */
    @Bean
    @ConditionalOnMissingBean(name = "easydocDocumentParser")
    @ConditionalOnProperty(name = "ddd4j.ai.document.enable-high-quality", havingValue = "true", matchIfMissing = true)
    public EasydocDocumentParser easydocDocumentParser() {
        return new EasydocDocumentParser();
    }

    /**
     * 装配 Excel 高质量委托解析器。
     *
     * @return Excel 解析器 Bean
     */
    @Bean
    @ConditionalOnMissingBean(name = "easyexcelDocumentParser")
    @ConditionalOnProperty(name = "ddd4j.ai.document.enable-high-quality", havingValue = "true", matchIfMissing = true)
    public EasyexcelDocumentParser easyexcelDocumentParser() {
        return new EasyexcelDocumentParser();
    }

    /**
     * 装配 ODF 高质量委托解析器。
     *
     * @return ODF 解析器 Bean
     */
    @Bean
    @ConditionalOnMissingBean(name = "easyodfDocumentParser")
    @ConditionalOnProperty(name = "ddd4j.ai.document.enable-high-quality", havingValue = "true", matchIfMissing = true)
    public EasyodfDocumentParser easyodfDocumentParser() {
        return new EasyodfDocumentParser();
    }
}
