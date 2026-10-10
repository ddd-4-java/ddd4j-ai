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
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public record ToolDefinition(
        String name,
        String description,
        Map<String, Object> parameters,
        ToolExecutor executor) {

    /**
     * 规范构造器：校验四要素均非 null。
     *
     * @param name        工具名称
     * @param description 工具描述
     * @param parameters  参数 JSON Schema
     * @param executor    执行回调
     * @throws NullPointerException 任一要素为 null 时
     */
    public ToolDefinition {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(parameters, "parameters must not be null");
        Objects.requireNonNull(executor, "executor must not be null");
    }

    /**
     * 工具执行回调（函数式接口）：接收 JSON 参数 Map，返回执行结果。
     */
    @FunctionalInterface
    public interface ToolExecutor {
        /**
         * 执行工具。
         *
         * @param arguments JSON 解析后的参数
         * @return 执行结果
         * @throws Exception 执行失败时原样抛出
         */
        Object execute(Map<String, Object> arguments) throws Exception;
    }
}
