package io.ddd4j.ai.cmpt.mcp.service;

import io.ddd4j.ai.cmpt.mcp.service.ToolDefinition.ToolExecutor;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link McpToolProvider} 端口契约测试：验证工具定义/调用约束。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class McpToolProviderContractTest {

    /** 桩实现：注册两个工具，一个抛异常用于验证错误路径。 */
    static class FakeToolProvider implements McpToolProvider {

        final ToolDefinition echo = new ToolDefinition(
                "echo",
                "回显输入字符串",
                Map.of("type", "object"),
                args -> args);

        final ToolDefinition fail = new ToolDefinition(
                "fail",
                "总是抛异常",
                Map.of(),
                args -> {
                    throw new IllegalStateException("intentional");
                });

        @Override
        public List<ToolDefinition> availableTools() {
            return List.of(echo, fail);
        }

        @Override
        public Object invokeTool(String name, Map<String, Object> arguments) throws Exception {
            return switch (name) {
                case "echo" -> echo.executor().execute(arguments);
                case "fail" -> fail.executor().execute(arguments);
                default -> throw new IllegalArgumentException("Unknown tool: " + name);
            };
        }
    }

    private final FakeToolProvider provider = new FakeToolProvider();

    @Test
    void availableTools_returnsAllRegisteredTools() {
        List<ToolDefinition> tools = provider.availableTools();
        assertThat(tools).hasSize(2);
        assertThat(tools).extracting(ToolDefinition::name).containsExactly("echo", "fail");
    }

    @Test
    void invokeTool_echoReturnsArguments() throws Exception {
        Map<String, Object> args = Map.of("message", "hello");
        Object result = provider.invokeTool("echo", args);
        assertThat(result).isEqualTo(args);
    }

    @Test
    void invokeTool_propagatesExecutorException() throws Exception {
        Map<String, Object> args = new HashMap<>();
        assertThatThrownBy(() -> provider.invokeTool("fail", args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("intentional");
    }

    @Test
    void invokeTool_unknownNameThrowsIllegalArgument() {
        assertThatThrownBy(() -> provider.invokeTool("missing", new HashMap<>()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void toolDefinition_constructorValidatesName() {
        ToolExecutor dummy = args -> null;
        assertThatThrownBy(() -> new ToolDefinition(null, "desc", Map.of(), dummy))
                .isInstanceOf(NullPointerException.class);
    }
}
