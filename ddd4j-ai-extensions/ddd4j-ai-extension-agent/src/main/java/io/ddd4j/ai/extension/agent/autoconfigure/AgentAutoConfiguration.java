package io.ddd4j.ai.extension.agent.autoconfigure;

import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
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

    @Bean
    @ConditionalOnMissingBean(HarnessAgent.class)
    @ConditionalOnBean(Model.class)
    public HarnessAgent harnessAgent(AgentProperties properties,
                                     ObjectProvider<Model> modelProvider,
                                     Toolkit toolkit) {
        HarnessAgent.Builder builder = HarnessAgent.builder()
                .name(properties.getName())
                .maxIters(properties.getMaxIterations())
                .toolkit(toolkit);
        Model model = modelProvider.getIfAvailable();
        if (model != null) {
            builder.model(model);
        }
        return builder.build();
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(AgentService.class)
    @ConditionalOnBean(HarnessAgent.class)
    public AgentService agentService(HarnessAgent harnessAgent) {
        return new AgentScopeAgentAdapter(harnessAgent);
    }
}
