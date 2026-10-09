package io.ddd4j.ai.extension.vectordb.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 向量数据库组件配置（前缀 {@code ddd4j.ai.vectordb}）。
 *
 * @author ddd4j
 */
@Getter
@Setter
@ConfigurationProperties(prefix = VectorDbProperties.PREFIX)
public class VectorDbProperties {

    /**
     * 默认构造器（配置对象由 Spring 绑定，字段均带默认值）。
     */
    public VectorDbProperties() {
    }

    /**
     * 配置属性前缀：{@code ddd4j.ai.vectordb}。
     */
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
