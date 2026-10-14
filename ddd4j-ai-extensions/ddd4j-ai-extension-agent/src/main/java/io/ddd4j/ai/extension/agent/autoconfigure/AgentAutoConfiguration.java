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
 * @author ddd4j
 */
@AutoConfiguration(afterName = "io.ddd4j.ai.extension.chat.autoconfigure.ChatAutoConfiguration")
@ConditionalOnClass({HarnessAgent.class, OpenAIChatModel.class})
@ConditionalOnProperty(name = "ddd4j.ai.agent.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AgentProperties.class)
public class AgentAutoConfiguration {

    /**
     * 将 {@code AgentProperties#getSubagents()} 声明转换为 Agentscope 子智能体声明列表（静态供测试）。
     *
     * @param properties 智能体配置属性
     * @return 转换后的子智能体声明列表，未配置或全部无效时返回空列表
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
     * 装配 Agentscope OpenAI 兼容 {@link Model}：仅在配置了
     * {@code ddd4j.ai.agent.api-key} 且业务未注入 {@link Model} Bean 时生效。
     *
     * @param properties 智能体配置属性（API Key、模型名、Base URL）
     * @return 构建完成的 {@link Model} 实例
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
     * 装配 Agentscope {@link Toolkit}，并将容器内全部 Spring AI
     * {@link ToolCallback} 注册为智能体工具。
     *
     * @param toolCallbacks 容器内的工具回调提供器，可为空
     * @return 装配完成的 {@link Toolkit}
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
     * 装配 MySQL 持久化的 {@link AgentStateStore}：仅在
     * {@code ddd4j.ai.agent.state-store=mysql} 且存在数据源时生效。
     *
     * @param dataSource 业务数据源
     * @return MySQL 实现的智能体状态存储
     */
    @Bean
    @ConditionalOnMissingBean(AgentStateStore.class)
    @ConditionalOnProperty(name = "ddd4j.ai.agent.state-store", havingValue = "mysql")
    public AgentStateStore agentStateStore(javax.sql.DataSource dataSource) {
        return new MysqlAgentStateStore(dataSource);
    }

    /**
     * 装配核心 {@link HarnessAgent}（ReAct 循环、任务清单、子智能体、状态存储）。
     *
     * @param properties        智能体配置属性
     * @param modelProvider     模型提供器，缺失时由 Builder 使用默认模型
     * @param toolkit           工具集
     * @param stateStoreProvider 状态存储提供器，未配置时省略状态持久化
     * @return 构建完成的 {@link HarnessAgent}
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
     * 以 {@link AgentScopeAgentAdapter} 薄包装 {@link HarnessAgent} 暴露主 {@link AgentService}。
     *
     * @param harnessAgent 已装配的核心智能体
     * @return 默认智能体服务 Bean
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(AgentService.class)
    @ConditionalOnBean(HarnessAgent.class)
    public AgentService agentService(HarnessAgent harnessAgent) {
        return new AgentScopeAgentAdapter(harnessAgent);
    }

    /**
     * 装配内存版派发任务仓储作为兜底实现（JDBC 仓储自动装配时退让）。
     *
     * @return 内存版派发任务仓储
     */
    @Bean
    @ConditionalOnMissingBean(io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepository.class)
    public io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepository agentDispatchTaskRepository() {
        return new io.ddd4j.ai.extension.agent.dispatch.InMemoryAgentDispatchTaskRepository();
    }

    /**
     * 装配计划编排器：按目标拆解计划并派发子任务给 HarnessAgent 执行。
     *
     * @param harnessAgent 已装配的核心智能体
     * @param repository   派发任务仓储
     * @return 计划编排器 Bean
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
     * @param executorProvider xxl-job 执行器提供器
     * @return xxl-job 实现的智能体调度器
     * @throws IllegalStateException 容器中缺少 {@code XxlJobExecutor} Bean 时抛出
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
