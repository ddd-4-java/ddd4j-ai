package io.ddd4j.ai.cmpt.document.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文档读取组件配置（前缀 {@code ddd4j.ai.document}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = DocumentProperties.PREFIX)
public class DocumentProperties {

    public static final String PREFIX = "ddd4j.ai.document";

    /**
     * 是否启用文档读取组件自动装配。
     */
    private boolean enabled = true;

    /**
     * 高质量优先：启用 4 组件委托解析器（easypdf/easydoc/easyexcel/easyodf）。
     */
    private boolean enableHighQuality = true;

    /**
     * 通用兜底：启用 Tika 基础解析器。
     */
    private boolean enableTikaFallback = true;

    /**
     * 启用 Tesseract OCR：扫描 PDF / 图片文字识别。宿主机需安装 tesseract 可执行文件，
     * 未安装时自动降级为无 OCR 解析（不中断）。
     */
    private boolean ocrEnabled = false;

    /**
     * OCR 语言（tesseract language pack，如 eng / chi_sim）。
     */
    private String ocrLanguage = "eng";

    /**
     * 启用语言检测（Tika LanguageDetector → metadata.language）。
     */
    private boolean enableLanguageDetection = true;

    /**
     * 文档大小上限（字节），超过直接拒绝（DocumentTooLargeException），防 OOM；{@code <=0} 不限制。
     */
    private long maxFileSizeBytes = 104_857_600L;

    /**
     * 解析超时（毫秒），超时抛 TikaTimeoutException 不重试；{@code <=0} 不限时。
     */
    private long parseTimeoutMillis = 60_000L;

    /**
     * 嵌入图片数量上限，超出丢弃并在 metadata 记 truncated；{@code <=0} 不限制。
     */
    private int maxEmbeddedImages = 20;

    /**
     * 单张嵌入图片大小上限（字节），超图跳过；{@code <=0} 不限制。
     */
    private long maxEmbeddedImageBytes = 5_242_880L;
}
