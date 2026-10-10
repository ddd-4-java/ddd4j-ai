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
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@AutoConfiguration
@ConditionalOnClass({PDFTextStripper.class, AutoDetectParser.class})
@ConditionalOnProperty(name = "ddd4j.ai.ocr.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OcrProperties.class)
public class OcrAutoConfiguration {

    /**
     * 构造 OcrAutoConfiguration 自动装配类（由 Spring 容器实例化）。
     */
    public OcrAutoConfiguration() {
    }

    /**
     * 注册端口层唯一识别服务：按媒体类型分派到 PDFBox / Tika。
     *
     * @param pdfBoxOcrService PDF 直提实现
     * @param tikaOcrService   多格式回退实现
     * @return 组合识别服务 bean
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(OcrService.class)
    public OcrService ocrService(PdfBoxOcrService pdfBoxOcrService, TikaOcrService tikaOcrService) {
        return new CompositeOcrService(pdfBoxOcrService, tikaOcrService);
    }

    /**
     * 注册 PDFBox 文本直提实现。
     *
     * @return PDFBox 识别服务 bean
     */
    @Bean
    public PdfBoxOcrService pdfBoxOcrService() {
        return new PdfBoxOcrService();
    }

    /**
     * 注册 Tika 多格式识别实现（是否启用 Tesseract 由配置决定）。
     *
     * @param properties 文档识别配置
     * @return Tika 识别服务 bean
     */
    @Bean
    public TikaOcrService tikaOcrService(OcrProperties properties) {
        return new TikaOcrService(properties.isOcrEnabled());
    }
}
