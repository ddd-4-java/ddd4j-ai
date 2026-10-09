package io.ddd4j.ai.extension.ocr.autoconfigure;

import io.ddd4j.ai.extension.ocr.properties.OcrProperties;
import io.ddd4j.ai.extension.ocr.service.OcrService;
import io.ddd4j.ai.extension.ocr.service.impl.CompositeOcrService;
import io.ddd4j.ai.extension.ocr.service.impl.PdfBoxOcrService;
import io.ddd4j.ai.extension.ocr.service.impl.TikaOcrService;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.parser.AutoDetectParser;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 文档识别自动装配：PDFBox 与 Tika 均就位时组合为单一端口实现，
 * 按媒体类型分派（PDF → PDFBox 直提，其余 → Tika）。
 *
 * @author ddd4j
 */
@AutoConfiguration
@ConditionalOnClass({PDFTextStripper.class, AutoDetectParser.class})
@ConditionalOnProperty(name = "ddd4j.ai.ocr.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OcrProperties.class)
public class OcrAutoConfiguration {

    /**
     * 默认构造器（自动装配类无状态）。
     */
    public OcrAutoConfiguration() {
    }

    /**
     * 装配组合识别端口：按媒体类型分派到 PDFBox 或 Tika。
     *
     * @param pdfBoxOcrService PDF 专用提取实现
     * @param tikaOcrService   多格式提取实现
     * @return 识别端口 Bean（{@code @Primary}）
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(OcrService.class)
    public OcrService ocrService(PdfBoxOcrService pdfBoxOcrService, TikaOcrService tikaOcrService) {
        return new CompositeOcrService(pdfBoxOcrService, tikaOcrService);
    }

    /**
     * 装配 PDFBox 提取实现。
     *
     * @return PDF 提取实现 Bean
     */
    @Bean
    public PdfBoxOcrService pdfBoxOcrService() {
        return new PdfBoxOcrService();
    }

    /**
     * 装配 Tika 多格式提取实现。
     *
     * @param properties 识别配置属性
     * @return Tika 提取实现 Bean
     */
    @Bean
    public TikaOcrService tikaOcrService(OcrProperties properties) {
        return new TikaOcrService(properties.isOcrEnabled());
    }
}
