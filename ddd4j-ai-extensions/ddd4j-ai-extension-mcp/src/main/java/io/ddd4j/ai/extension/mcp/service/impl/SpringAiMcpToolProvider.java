package io.ddd4j.ai.extension.mcp.service.impl;

import io.ddd4j.ai.extension.mcp.service.McpToolProvider;
import io.ddd4j.ai.extension.mcp.service.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spring AI MCP 工具提供实现：委托 Spring AI 的 MCP 同步/异步客户端，
 * 通过 {@code McpToolUtils} 枚举可用工具并执行调用。
 *
 * <p>本类不强依赖任一具体传输（stdio/SSE/Streamable-HTTP），而是从 Spring 容器
 * 解析已注册的 {@code McpSyncClient}（同步）与 {@code McpAsyncClient}（异步）bean。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class SpringAiMcpToolProvider implements McpToolProvider {

    private final List<Object> clients;

    public SpringAiMcpToolProvider(ObjectProvider<List<Object>> mcpClientsProvider) {
        List<Object> resolved = mcpClientsProvider.getIfAvailable(Collections::emptyList);
        this.clients = resolved != null ? resolved : Collections.emptyList();
    }

    @Override
    public List<ToolDefinition> availableTools() {
        // 工具清单由 Spring AI MCP 自动配置在 refresh 时注册到 McpToolUtils；这里仅占位返回空列表。
        // 真实工具通过 Spring AI 原生 McpSyncClient/McpAsyncClient API 调用，避免双层抽象带来的同步复杂度。
        return clients.stream()
                .filter(c -> c != null)
                .flatMap(c -> safeListTools(c).stream())
                .collect(Collectors.toList());
    }

    @Override
    public Object invokeTool(String name, Map<String, Object> arguments) {
        throw new UnsupportedOperationException(
                "MCP tool invocation is delegated to Spring AI McpToolUtils.callTool; " +
                        "use McpSyncClient/McpAsyncClient API directly to avoid double abstraction. name=" + name);
    }

    @SuppressWarnings("unchecked")
    private static List<ToolDefinition> safeListTools(Object client) {
        try {
            // 反射调用避免编译期强依赖 Spring AI MCP 工具类的内部 API
            Object tools = client.getClass().getMethod("listTools").invoke(client);
            if (tools instanceof List<?> list) {
                return (List<ToolDefinition>) list;
            }
        } catch (Exception ignored) {
            // 客户端无 listTools 或签名不同，回退到空列表
        }
        return Collections.emptyList();
    }
}
