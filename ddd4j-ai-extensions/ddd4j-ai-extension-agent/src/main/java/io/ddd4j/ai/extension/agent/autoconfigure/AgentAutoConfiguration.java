package io.ddd4j.ai.extension.agent.autoconfigure;

import io.agentscope.core.model.Model;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.mysql.state.MysqlAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import io.ddd4j.ai.extension.agent.agent.AgentScopeAgentAdapter;
import io.ddd4j.ai.extension.agent.agent.SpringAiToolkitBuilder;
import io.ddd4j.ai.extension.agent.properties.AgentProperties;
import io.ddd4j.ai.extension.agent.service.AgentService;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * 智能体自动装配：暴露 Agentscope {@link HarnessAgent}（ReAct + 多智能体 + 记忆压缩）
 * 与 Agentscope {@link Toolkit}（Spring AI ToolCallback 兼容注册）为业务可注入 Bean；
 * 默认 {@link AgentService} 端口由 {@link AgentScopeAgentAdapter} 薄包装 HarnessAgent 提供。
 *
 * <p>装配条件：需要 OpenAI 兼容 API Key（{@code ddd4j.ai.agent.api-key}）或业务侧注入
 * {@link Model} Bean；否则回退不装配（业务侧无 LLM 凭证时静默跳过）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@AutoConfiguration(afterName = "io.ddd4j.ai.extension.chat.autoconfigure.ChatAutoConfiguration")
@ConditionalOnClass({HarnessAgent.class, OpenAIChatModel.class})
@ConditionalOnProperty(name = "ddd4j.ai.agent.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AgentProperties.class)
public class AgentAutoConfiguration {

    /**
     * 构造 AgentAutoConfiguration 自动装配类（由 Spring 容器实例化）。
     */
    public AgentAutoConfiguration() {
    }

    /**
     * 构建 Agentscope OpenAI 兼容 {@link Model}（仅当业务侧未自行注入时生效）。
     *
     * @param properties 智能体装配配置（API Key、模型名、Base URL）
     * @return 构建完成的 {@link OpenAIChatModel} 模型实例
     */
    @Bean
    @ConditionalOnMissingBean(Model.class)
    @ConditionalOnProperty(name = "ddd4j.ai.agent.api-key")
    public Model agentscopeModel(AgentProperties properties) {
        OpenAIChatModel.Builder builder = OpenAIChatModel.builder()
                .apiKey(properties.getApiKey())
                .modelName(properties.getModelName());
        if (properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()) {
            builder.baseUrl(properties.getBaseUrl());
        }
        return builder.build();
    }

    /**
     * 构建 Agentscope {@link Toolkit}，把容器内的 Spring AI {@link ToolCallback}
     * 全部经 {@link SpringAiToolkitBuilder#registerSpringAiTools} 注册进去。
     *
     * @param toolCallbacks 容器内全部工具回调的延迟提供器；缺失或为空时返回空 Toolkit
     * @return 注册完成的 {@link Toolkit} 实例
     */
    @Bean
    @ConditionalOnMissingBean(Toolkit.class)
    public Toolkit agentscopeToolkit(ObjectProvider<List<ToolCallback>> toolCallbacks) {
        Toolkit toolkit = new Toolkit();
        List<ToolCallback> callbacks = toolCallbacks.getIfAvailable();
        if (callbacks != null && !callbacks.isEmpty()) {
            SpringAiToolkitBuilder.registerSpringAiTools(callbacks, toolkit);
        }
        return toolkit;
    }

    /**
     * 构建 MySQL 持久化的智能体状态存储（需配置 {@code ddd4j.ai.agent.state-store=mysql}）。
     *
     * @param dataSource 应用主数据源
     * @return 基于数据源的 {@link AgentStateStore} 实例
     */
    @Bean
    @ConditionalOnMissingBean(AgentStateStore.class)
    @ConditionalOnProperty(name = "ddd4j.ai.agent.state-store", havingValue = "mysql")
    public AgentStateStore agentStateStore(javax.sql.DataSource dataSource) {
        return new MysqlAgentStateStore(dataSource);
    }

    /**
     * 构建核心 {@link HarnessAgent}（ReAct 循环 + 工具集 + 可选子智能体与状态存储）。
     *
     * @param properties       智能体配置（名称、最大迭代轮次、任务清单开关、子智能体声明）
     * @param modelProvider    模型延迟提供器；无模型时交由 HarnessAgent 内部默认行为
     * @param toolkit          工具集（已注册 Spring AI 工具）
     * @param stateStoreProvider 状态存储延迟提供器；未配置时使用内存态
     * @return 构建完成的 {@link HarnessAgent} 实例
     */
    @Bean
    @ConditionalOnMissingBean(HarnessAgent.class)
    @ConditionalOnBean(Model.class)
    public HarnessAgent harnessAgent(AgentProperties properties,
                                     ObjectProvider<Model> modelProvider,
                                     Toolkit toolkit,
                                     ObjectProvider<AgentStateStore> stateStoreProvider) {
        HarnessAgent.Builder builder = HarnessAgent.builder()
                .name(properties.getName())
                .maxIters(properties.getMaxIterations())
                .toolkit(toolkit)
                .enableTaskList(properties.isTaskListEnabled());
        AgentStateStore stateStore = stateStoreProvider.getIfAvailable();
        if (stateStore != null) {
            builder.stateStore(stateStore);
        }
        List<io.agentscope.harness.agent.subagent.SubagentDeclaration> subagents = parseSubagents(properties);
        if (!subagents.isEmpty()) {
            builder.subagents(subagents);
        }
        Model model = modelProvider.getIfAvailable();
        if (model != null) {
            builder.model(model);
        }
        return builder.build();
    }

    /**
     * AgentProperties.subagents → Agentscope SubagentDeclaration 列表（静态供测试）。
     *
     * @param properties 智能体配置，读取其 {@code subagents} 声明列表
     * @return 过滤掉无名声明后映射得到的子智能体声明列表；无配置时返回空列表
     */
    public static List<io.agentscope.harness.agent.subagent.SubagentDeclaration> parseSubagents(
            AgentProperties properties) {
        if (properties.getSubagents() == null || properties.getSubagents().isEmpty()) {
            return List.of();
        }
        return properties.getSubagents().stream()
                .filter(spec -> spec.getName() != null && !spec.getName().isBlank())
                .map(spec -> {
                    io.agentscope.harness.agent.subagent.SubagentDeclaration.Builder sub =
                            io.agentscope.harness.agent.subagent.SubagentDeclaration.builder()
                                    .name(spec.getName());
                    if (spec.getDescription() != null) {
                        sub.description(spec.getDescription());
                    }
                    if (spec.getInlineAgentsBody() != null) {
                        sub.inlineAgentsBody(spec.getInlineAgentsBody());
                    }
                    if (spec.getModel() != null) {
                        sub.model(spec.getModel());
                    }
                    if (spec.getMaxIters() != null) {
                        sub.maxIters(spec.getMaxIters());
                    }
                    return sub.build();
                })
                .toList();
    }

    /**
     * 以 {@link AgentScopeAgentAdapter} 薄包装 {@link HarnessAgent} 暴露 {@link AgentService} 端口。
     *
     * @param harnessAgent 已装配的核心智能体
     * @return 面向业务的 {@link AgentService} 实现
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(AgentService.class)
    @ConditionalOnBean(HarnessAgent.class)
    public AgentService agentService(HarnessAgent harnessAgent) {
        return new AgentScopeAgentAdapter(harnessAgent);
    }

    /**
     * 装配内存版调度任务仓储（缺省实现，可通过业务 Bean 覆盖）。
     *
     * @return 内存版 {@link io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepository}
     */
    @Bean
    @ConditionalOnMissingBean(io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepository.class)
    public io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepository agentDispatchTaskRepository() {
        return new io.ddd4j.ai.extension.agent.dispatch.InMemoryAgentDispatchTaskRepository();
    }

    /**
     * 装配任务编排器：把长目标调度任务交给 HarnessAgent 分步推进并持久化进度。
     *
     * @param harnessAgent 已装配的核心智能体
     * @param repository   调度任务仓储（进度读写）
     * @return 任务编排器实例
     */
    @Bean
    @ConditionalOnMissingBean(io.ddd4j.ai.extension.agent.dispatch.AgentPlanOrchestrator.class)
    @ConditionalOnBean(HarnessAgent.class)
    public io.ddd4j.ai.extension.agent.dispatch.AgentPlanOrchestrator agentPlanOrchestrator(
            HarnessAgent harnessAgent,
            io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepository repository) {
        return new io.ddd4j.ai.extension.agent.dispatch.AgentPlanOrchestrator(harnessAgent, repository);
    }

    /**
     * Agentscope 调度器（xxl-job 实现）：定时触发智能体任务（长目标续跑）。
     * 需业务装配 {@code com.xxl.job.core.executor.XxlJobExecutor} 并开启
     * {@code ddd4j.ai.agent.scheduler=xxl-job}；后续 agent-job 调度器以同样方式插入。
     *
     * @param executorProvider XXL-Job 执行器的延迟提供器；缺失时抛出 {@link IllegalStateException}
     * @return 基于 XXL-Job 的 {@link io.agentscope.extensions.scheduler.AgentScheduler}
     * @throws IllegalStateException 当未装配 {@code XxlJobExecutor} Bean 时
     */
    @Bean
    @ConditionalOnMissingBean(io.agentscope.extensions.scheduler.AgentScheduler.class)
    @ConditionalOnProperty(name = "ddd4j.ai.agent.scheduler", havingValue = "xxl-job")
    public io.agentscope.extensions.scheduler.AgentScheduler agentScheduler(
            ObjectProvider<com.xxl.job.core.executor.XxlJobExecutor> executorProvider) {
        com.xxl.job.core.executor.XxlJobExecutor executor = executorProvider.getIfAvailable();
        if (executor == null) {
            throw new IllegalStateException(
                    "ddd4j.ai.agent.scheduler=xxl-job 需要业务装配 XxlJobExecutor Bean");
        }
        return new io.agentscope.extensions.scheduler.xxljob.XxlJobAgentScheduler(executor);
    }
}
