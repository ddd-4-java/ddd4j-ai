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

    /**
     * 是否启用内置任务清单（HarnessAgent enableTaskList：智能体可自建/追踪任务）。
     */
    private boolean taskListEnabled = false;

    /**
     * 声明式子智能体（多智能体派发）；每项映射为 Agentscope SubagentDeclaration。
     */
    private java.util.List<SubagentSpec> subagents = new java.util.ArrayList<>();

    /**
     * 状态持久化：none（默认，内存态）/ mysql（MysqlAgentStateStore，需业务 DataSource）。
     */
    private String stateStore = "none";

    /**
     * 调度器：none（默认）/ xxl-job（需业务装配 XxlJobExecutor）。
     */
    private String scheduler = "none";

    /**
     * 子智能体声明（配置文件友好）。
     */
    @Getter
    @Setter
    public static class SubagentSpec {

        /** 子智能体名称（唯一标识）。 */
        private String name;

        /** 描述（供父智能体路由决策）。 */
        private String description;

        /** 内联智能体指令体（system prompt 片段；与 model 二选一路径）。 */
        private String inlineAgentsBody;

        /** 子智能体模型名（可选，覆盖父模型）。 */
        private String model;

        /** 子智能体最大迭代次数（默认继承父配置）。 */
        private Integer maxIters;
    }
}
