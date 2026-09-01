package io.ddd4j.ai.extension.mcp.service;

import java.util.List;

/**
 * MCP 工具提供端口：屏蔽 Spring AI MCP 客户端/服务端协议细节，
 * 业务侧仅面对统一的工具发现/调用契约。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface McpToolProvider {

    /**
     * 列出所有可发现的工具定义。
     */
    List<ToolDefinition> availableTools();

    /**
     * 调用指定工具。
     *
     * @param name      工具名称（必须来自 {@link #availableTools()}）
     * @param arguments 工具参数（与 ToolDefinition 的 JSON schema 对应）
     * @return 工具执行结果（任意可序列化对象）
     * @throws Exception 允许底层适配器透传任何异常（如 IO 错误、MCP 协议错误）
     */
    Object invokeTool(String name, java.util.Map<String, Object> arguments) throws Exception;
}
