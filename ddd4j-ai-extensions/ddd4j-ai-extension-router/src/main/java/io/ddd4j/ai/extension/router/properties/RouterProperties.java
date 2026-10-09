package io.ddd4j.ai.extension.router.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * 多模型路由组件配置（前缀 {@code ddd4j.ai.router}）。
 *
 * @author ddd4j
 */
@Getter
@Setter
@ConfigurationProperties(prefix = RouterProperties.PREFIX)
public class RouterProperties {

    /**
     * 默认构造器（配置属性类由 Spring 绑定）。
     */
    public RouterProperties() {
    }

    /** 配置属性前缀。 */
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

    /**
     * 路由策略类型。
     */
    public enum StrategyType {
        /** 轮询：依次循环候选模型。 */
        ROUND_ROBIN,
        /** 权重：按权重比例随机分发。 */
        WEIGHTED,
        /** 延迟：优先最近响应最快的模型。 */
        LATENCY
    }
}
