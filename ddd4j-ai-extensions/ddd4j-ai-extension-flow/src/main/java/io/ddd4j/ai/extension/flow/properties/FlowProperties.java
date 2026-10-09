package io.ddd4j.ai.extension.flow.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 工作流组件配置（前缀 {@code ddd4j.ai.flow}）。
 *
 * @author ddd4j
 */
@Getter
@Setter
@ConfigurationProperties(prefix = FlowProperties.PREFIX)
public class FlowProperties {

    /**
     * 默认构造器（配置属性类由 Spring 绑定）。
     */
    public FlowProperties() {
    }

    /** 配置属性前缀。 */
    public static final String PREFIX = "ddd4j.ai.flow";

    /**
     * 是否启用工作流组件自动装配。
     */
    private boolean enabled = true;
}
