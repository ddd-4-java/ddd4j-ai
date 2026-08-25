package io.ddd4j.ai.cmpt.mcp.autoconfigure;

import io.ddd4j.ai.cmpt.mcp.service.McpToolProvider;
import io.ddd4j.ai.cmpt.mcp.service.impl.SpringAiMcpToolProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;

/**
 * MCP 组件自动装配：容器中存在任一 Spring AI MCP 客户端 bean
 * （{@code McpSyncClient} 或 {@code McpAsyncClient}）时激活。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
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

    @org.springframework.context.annotation.Bean
    @ConditionalOnClass(name = "org.springframework.ai.mcp.client.McpSyncClient")
    public McpToolProvider mcpToolProvider(ObjectProvider<List<Object>> mcpClientsProvider) {
        return new SpringAiMcpToolProvider(mcpClientsProvider);
    }
}
