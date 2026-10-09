package io.ddd4j.ai.extension.mcp.autoconfigure;

import io.ddd4j.ai.extension.mcp.service.McpToolProvider;
import io.ddd4j.ai.extension.mcp.service.impl.SpringAiMcpToolProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;

/**
 * MCP 组件自动装配：容器中存在任一 Spring AI MCP 客户端 bean
 * （{@code McpSyncClient} 或 {@code McpAsyncClient}）时激活。
 *
 * @author ddd4j
 */
@AutoConfiguration(afterName = {
        "org.springframework.ai.model.mcp.client.autoconfigure.McpClientAutoConfiguration",
        "org.springframework.ai.model.mcp.client.autoconfigure.autoconfigure.McpClientAutoConfiguration"
})
@ConditionalOnClass(name = {
        "org.springframework.ai.mcp.client.McpSyncClient",
        "org.springframework.ai.mcp.client.McpAsyncClient"
})
@ConditionalOnProperty(name = "ddd4j.ai.mcp.enabled", havingValue = "true", matchIfMissing = true)
public class McpAutoConfiguration {

    /**
     * 默认构造器（自动装配类无状态）。
     */
    public McpAutoConfiguration() {
    }

    /**
     * 装配 MCP 工具提供端口（聚合容器内全部 MCP 客户端）。
     *
     * @param mcpClientsProvider MCP 客户端列表提供器
     * @return MCP 工具提供端口 Bean
     */
    @org.springframework.context.annotation.Bean
    @ConditionalOnClass(name = "org.springframework.ai.mcp.client.McpSyncClient")
    public McpToolProvider mcpToolProvider(ObjectProvider<List<Object>> mcpClientsProvider) {
        return new SpringAiMcpToolProvider(mcpClientsProvider);
    }
}
