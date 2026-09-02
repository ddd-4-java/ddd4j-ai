package io.ddd4j.ai.extension.flow.autoconfigure;

import com.alibaba.cloud.ai.graph.StateGraph;
import io.ddd4j.ai.extension.chat.service.ChatService;
import io.ddd4j.ai.extension.flow.properties.FlowProperties;
import io.ddd4j.ai.extension.flow.service.FlowService;
import io.ddd4j.ai.extension.flow.service.impl.GraphFlowService;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 工作流自动装配：依赖对话端口与图引擎类，可选注入工具回调集合。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@AutoConfiguration(afterName = {
        "io.ddd4j.ai.extension.chat.autoconfigure.ChatAutoConfiguration",
        "io.ddd4j.ai.extension.agent.autoconfigure.AgentAutoConfiguration"})
@ConditionalOnClass(StateGraph.class)
@ConditionalOnBean(ChatService.class)
@ConditionalOnProperty(name = "ddd4j.ai.flow.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(FlowProperties.class)
public class FlowAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(FlowService.class)
    public FlowService flowService(ChatService chatService,
                                   ObjectProvider<List<ToolCallback>> toolCallbacks,
                                   ObjectProvider<io.ddd4j.ai.extension.agent.service.AgentService> agentService) {
        return new GraphFlowService(chatService, toolCallbacks.getIfAvailable(List::of),
                agentService.getIfAvailable());
    }
}
