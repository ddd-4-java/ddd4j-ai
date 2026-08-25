package io.ddd4j.ai.samples.mcp;

import io.ddd4j.ai.cmpt.mcp.service.McpToolProvider;
import io.ddd4j.ai.cmpt.mcp.service.ToolDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * mcp 模型上下文组件使用示例：工具注册表查询与工具调用。
 *
 * <p>前提：业务服务引入 {@code spring-ai-starter-mcp-client} 与 MCP Server 连接配置
 * （参考 Spring AI MCP 文档），再引入 {@code ddd4j-ai-extension-mcp}。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Component
public class McpSample {

    private final McpToolProvider toolProvider;

    public McpSample(McpToolProvider toolProvider) {
        this.toolProvider = toolProvider;
    }

    /** 列出全部可用 MCP 工具（供 LLM 提示词注入 / 诊断）。 */
    public List<ToolDefinition> availableTools() {
        return toolProvider.availableTools();
    }

    /** 调用指定工具并返回执行结果。 */
    public Object invoke(String toolName, Map<String, Object> arguments) throws Exception {
        return toolProvider.invokeTool(toolName, arguments);
    }
}
