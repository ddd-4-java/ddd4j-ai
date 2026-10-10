package io.ddd4j.ai.extension.embedding.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 向量嵌入组件配置（前缀 {@code ddd4j.ai.embedding}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = EmbeddingProperties.PREFIX)
public class EmbeddingProperties {

    /**
     * 构造 EmbeddingProperties 配置（由 Spring 容器绑定属性时实例化）。
     */
    public EmbeddingProperties() {
    }

    /** 配置前缀（{@code ddd4j.ai.embedding}）。 */
    public static final String PREFIX = "ddd4j.ai.embedding";

    /**
     * 是否启用向量嵌入组件自动装配。
     */
    private boolean enabled = true;

    /**
     * 批量嵌入的单批大小：超过该值的输入将被分片调用底层模型。
     */
    private int batchSize = 32;
}
