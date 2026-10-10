package io.ddd4j.ai.extension.router.properties;

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

    /**
     * 构造 RouterProperties 配置（由 Spring 容器绑定属性时实例化）。
     */
    public RouterProperties() {
    }

    /** 配置前缀（{@code ddd4j.ai.router}）。 */
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
     * 路由策略枚举：轮询 / 权重 / 延迟探测。
     */
    public enum StrategyType {
        /** 轮询：按序循环分发（默认）。 */
        ROUND_ROBIN,
        /** 权重：按权重表加权随机分发。 */
        WEIGHTED,
        /** 延迟：按各模型响应时延择优分发。 */
        LATENCY
    }
}
