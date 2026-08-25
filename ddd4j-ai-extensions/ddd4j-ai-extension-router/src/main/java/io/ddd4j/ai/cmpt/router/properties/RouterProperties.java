package io.ddd4j.ai.cmpt.router.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * 多模型路由组件配置（前缀 {@code ddd4j.ai.router}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = RouterProperties.PREFIX)
public class RouterProperties {

    public static final String PREFIX = "ddd4j.ai.router";

    /**
     * 是否启用路由组件自动装配。
     */
    private boolean enabled = true;

    /**
     * 选择策略：轮询（默认）/ 权重 / 延迟。
     */
    private StrategyType strategy = StrategyType.ROUND_ROBIN;

    /**
     * 权重策略的模型权重表（模型 id → 权重）。
     */
    private Map<String, Integer> weights = Map.of();

    public enum StrategyType {
        ROUND_ROBIN,
        WEIGHTED,
        LATENCY
    }
}
