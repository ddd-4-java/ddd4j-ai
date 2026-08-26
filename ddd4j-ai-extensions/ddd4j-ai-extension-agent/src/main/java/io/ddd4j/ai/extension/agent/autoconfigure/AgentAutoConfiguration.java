package io.ddd4j.ai.extension.agent.autoconfigure;

import io.ddd4j.ai.extension.agent.properties.AgentProperties;
import io.ddd4j.ai.extension.agent.service.AgentService;
import io.ddd4j.ai.extension.agent.service.impl.PlanExecuteAgent;
import io.ddd4j.ai.extension.agent.service.impl.ReActAgent;
import io.ddd4j.ai.extension.chat.service.ChatService;
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
 * 智能体自动装配：依赖对话端口 {@link ChatService}，可选注入工具回调集合；
 * ReAct 为默认端口实现（{@code @Primary}），Plan-Execute 按类型独立注入。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@AutoConfiguration(afterName = "io.ddd4j.ai.extension.chat.autoconfigure.ChatAutoConfiguration")
@ConditionalOnClass(ChatService.class)
@ConditionalOnBean(ChatService.class)
@ConditionalOnProperty(name = "ddd4j.ai.agent.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AgentProperties.class)
public class AgentAutoConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean(AgentService.class)
    public ReActAgent reActAgent(ChatService chatService,
                                 ObjectProvider<List<ToolCallback>> toolCallbacks,
                                 AgentProperties properties) {
        return new ReActAgent(chatService, toolCallbacks.getIfAvailable(List::of), properties.getMaxIterations());
    }

    @Bean
    public PlanExecuteAgent planExecuteAgent(ChatService chatService, AgentProperties properties) {
        return new PlanExecuteAgent(chatService, properties.getMaxPlanSteps());
    }
}
