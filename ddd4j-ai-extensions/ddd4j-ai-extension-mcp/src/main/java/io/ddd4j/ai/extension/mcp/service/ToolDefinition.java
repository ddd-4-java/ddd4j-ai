package io.ddd4j.ai.extension.mcp.service;

import java.beans.ConstructorProperties;

import java.util.Map;
import java.util.Objects;

/**
 * MCP 工具定义（端口契约）：屏蔽底层 MCP 协议细节，仅暴露 name/description/parameters/execute。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */

public final class ToolDefinition {

    private static final long serialVersionUID = 0L;

    private final String name;

    private final String description;

    private final Map<String, Object> parameters;

    private final ToolExecutor executor;

    /**
 * @param name 工具名称（路由与诊断标识）
 * @param description 工具描述（注入 LLM prompt）
 * @param parameters JSON Schema 形式参数 schema（key: 参数名, value: 类型描述）
 * @param executor 工具执行回调（接收 JSON 参数 Map，返回执行结果）
 */

    @ConstructorProperties({ "name", "description", "parameters", "executor" })
    public ToolDefinition(String name, String description, Map<String, Object> parameters, ToolExecutor executor) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(parameters, "parameters must not be null");
        Objects.requireNonNull(executor, "executor must not be null");
        this.name = name;
        this.description = description;
        this.parameters = parameters;
        this.executor = executor;
    }

    public interface ToolExecutor {

        Object execute(Map<String, Object> arguments) throws Exception;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Map<String, Object> parameters() {
        return parameters;
    }

    public ToolExecutor executor() {
        return executor;
    }

    public String getName() {
        return name();
    }

    public String getDescription() {
        return description();
    }

    public Map<String, Object> getParameters() {
        return parameters();
    }

    public ToolExecutor getExecutor() {
        return executor();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        ToolDefinition other = (ToolDefinition) obj;
        return Objects.equals(this.name, other.name) && Objects.equals(this.description, other.description) && Objects.equals(this.parameters, other.parameters) && Objects.equals(this.executor, other.executor);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(name);
        result = 31 * result + Objects.hashCode(description);
        result = 31 * result + Objects.hashCode(parameters);
        result = 31 * result + Objects.hashCode(executor);
        return result;
    }

    @Override
    public String toString() {
        return "ToolDefinition[name=" + name + ", description=" + description + ", parameters=" + parameters + ", executor=" + executor + "]";
    }
}
