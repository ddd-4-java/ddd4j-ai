package io.ddd4j.ai.extension.ocr.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文档识别组件配置（前缀 {@code ddd4j.ai.ocr}）。
 *
 * @author ddd4j
 */
@Getter
@Setter
@ConfigurationProperties(prefix = OcrProperties.PREFIX)
public class OcrProperties {

    /**
     * 默认构造器（配置属性类由 Spring 绑定）。
     */
    public OcrProperties() {
    }

    /** 配置属性前缀。 */
    public static final String PREFIX = "ddd4j.ai.ocr";

    /**
     * 是否启用文档识别组件自动装配。
     */
    private boolean enabled = true;

    /**
     * 是否启用 Tesseract OCR 图像扫描；依赖宿主机安装 tesseract 可执行文件，
     * 未安装时解析图像会失败。默认关闭（仅提取嵌入式文本与元数据）。
     */
    private boolean ocrEnabled = false;
}
