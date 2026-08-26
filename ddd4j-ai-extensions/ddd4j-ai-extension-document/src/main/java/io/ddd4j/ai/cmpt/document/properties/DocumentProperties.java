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
}
