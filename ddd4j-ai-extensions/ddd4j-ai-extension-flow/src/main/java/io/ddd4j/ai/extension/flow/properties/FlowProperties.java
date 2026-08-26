package io.ddd4j.ai.extension.flow.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 工作流组件配置（前缀 {@code ddd4j.ai.flow}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = FlowProperties.PREFIX)
public class FlowProperties {

    public static final String PREFIX = "ddd4j.ai.flow";

    /**
     * 是否启用工作流组件自动装配。
     */
    private boolean enabled = true;
}
