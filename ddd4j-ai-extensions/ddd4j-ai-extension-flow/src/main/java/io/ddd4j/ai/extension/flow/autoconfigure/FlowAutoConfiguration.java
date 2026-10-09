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
 * @author ddd4j
 */
@AutoConfiguration(afterName = {
        "io.ddd4j.ai.extension.chat.autoconfigure.ChatAutoConfiguration",
        "io.ddd4j.ai.extension.agent.autoconfigure.AgentAutoConfiguration"})
@ConditionalOnClass(StateGraph.class)
@ConditionalOnBean(ChatService.class)
@ConditionalOnProperty(name = "ddd4j.ai.flow.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(FlowProperties.class)
public class FlowAutoConfiguration {

    /**
     * 默认构造器（自动装配类无状态）。
     */
    public FlowAutoConfiguration() {
    }

    /**
     * 装配工作流服务（图引擎编译执行声明式 DSL）。
     *
     * @param chatService      对话端口
     * @param toolCallbacks    工具回调提供器（可为空）
     * @param agentService     智能体服务提供器（可为空，AGENT 节点不可用）
     * @return 工作流服务 Bean
     */
    @Bean
    @ConditionalOnMissingBean(FlowService.class)
    public FlowService flowService(ChatService chatService,
                                   ObjectProvider<List<ToolCallback>> toolCallbacks,
                                   ObjectProvider<io.ddd4j.ai.extension.agent.service.AgentService> agentService) {
        return new GraphFlowService(chatService, toolCallbacks.getIfAvailable(List::of),
                agentService.getIfAvailable());
    }
}
