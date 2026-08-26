package io.ddd4j.ai.extension.vectordb.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 向量数据库组件配置（前缀 {@code ddd4j.ai.vectordb}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = VectorDbProperties.PREFIX)
public class VectorDbProperties {

    public static final String PREFIX = "ddd4j.ai.vectordb";

    /**
     * 是否启用向量数据库组件自动装配。
     */
    private boolean enabled = true;

    /**
     * 默认检索条数上限（search 未显式指定 topK 时使用）。
     */
    private int defaultTopK = 4;
}
