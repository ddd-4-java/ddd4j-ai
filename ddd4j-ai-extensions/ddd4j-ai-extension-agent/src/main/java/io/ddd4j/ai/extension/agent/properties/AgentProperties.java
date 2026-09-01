package io.ddd4j.ai.extension.agent.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 智能体组件配置（前缀 {@code ddd4j.ai.agent}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = AgentProperties.PREFIX)
public class AgentProperties {

    public static final String PREFIX = "ddd4j.ai.agent";

    /**
     * 是否启用智能体组件自动装配。
     */
    private boolean enabled = true;

    /**
     * ReAct 最大思考-动作迭代轮数；超限抛异常防死循环。
     */
    private int maxIterations = 5;

    /**
     * Plan-Execute 最大计划执行步数。
     */
    private int maxPlanSteps = 5;
}
