package io.ddd4j.ai.extension.mcp.service;

import java.util.Map;
import java.util.Objects;

/**
 * MCP 工具定义（端口契约）：屏蔽底层 MCP 协议细节，仅暴露 name/description/parameters/execute。
 *
 * @param name        工具名称（路由与诊断标识）
 * @param description 工具描述（注入 LLM prompt）
 * @param parameters  JSON Schema 形式参数 schema（key: 参数名, value: 类型描述）
 * @param executor    工具执行回调（接收 JSON 参数 Map，返回执行结果）
 * @author ddd4j
 */
public record ToolDefinition(
        String name,
        String description,
        Map<String, Object> parameters,
        ToolExecutor executor) {

    /**
     * 压缩构造器：校验四个组件均非空。
     *
     * @throws NullPointerException 任一组件为 {@code null}
     */
    public ToolDefinition {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(parameters, "parameters must not be null");
        Objects.requireNonNull(executor, "executor must not be null");
    }

    /**
     * 工具执行回调：接收参数 Map，返回执行结果。
     */
    public interface ToolExecutor {

        /**
         * 执行工具。
         *
         * @param arguments 工具参数
         * @return 执行结果
         * @throws Exception 执行期任意异常
         */
        Object execute(Map<String, Object> arguments) throws Exception;
    }
}
