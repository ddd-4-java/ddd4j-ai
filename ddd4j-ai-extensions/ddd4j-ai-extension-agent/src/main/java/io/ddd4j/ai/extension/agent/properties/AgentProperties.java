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

    /**
     * 智能体名称（Agentscope HarnessAgent 标识）。
     */
    private String name = "default-agent";

    /**
     * 模型名称（OpenAI 兼容协议，如 gpt-4o-mini / qwen-plus / ollama 模型名）。
     */
    private String modelName = "gpt-4o-mini";

    /**
     * OpenAI 兼容模型 API Key；为空时不装配 HarnessAgent（需业务注入 Model Bean 兜底）。
     */
    private String apiKey;

    /**
     * OpenAI 兼容模型 Base URL（如 https://api.deepseek.com / Ollama 本地地址）。
     */
    private String baseUrl;
}
