package io.ddd4j.ai.extension.memory.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 会话记忆配置（前缀 {@code ddd4j.ai.memory}）。
 *
 * @author ddd4j
 */
@Getter
@Setter
@ConfigurationProperties(prefix = MemoryProperties.PREFIX)
public class MemoryProperties {

    /**
     * 默认构造器（配置属性类由 Spring 绑定）。
     */
    public MemoryProperties() {
    }

    /** 配置属性前缀。 */
    public static final String PREFIX = "ddd4j.ai.memory";

    /**
     * 是否启用记忆组件自动装配。
     */
    private boolean enabled = true;

    /**
     * 窗口大小：每个会话保留最近 N 条消息。
     */
    private int windowSize = 20;
}
