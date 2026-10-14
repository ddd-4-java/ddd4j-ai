package io.ddd4j.ai.extension.flow.service;

import java.beans.ConstructorProperties;

import java.util.Map;
import java.util.Objects;

/**
 * 工作流节点定义。
 *
 * @author ddd4j
 */

public final class FlowNodeSpec {

    private static final long serialVersionUID = 0L;

    private final String id;

    private final FlowNodeType type;

    private final String prompt;

    private final String toolName;

    private final String inputKey;

    private final String outputKey;

    private final Map<String, String> branches;

    /**
     * 压缩构造器：校验 id/type 非空，branches 空值归一为空表。
     *
     * @param id 节点标识（图内唯一）
     * @param type 节点类型
     * @param prompt LLM 节点提示词；{key} 占位符由 state 同名字段替换
     * @param toolName 工具节点目标工具名
     * @param inputKey TOOL/BRANCH 节点读取的 state 字段
     * @param outputKey LLM/TOOL 节点写入结果的 state 字段
     * @param branches BRANCH 节点路由表：state 值 → 下一节点 id
     * @throws NullPointerException {@code id}/{@code type} 为 {@code null}
     */

    @ConstructorProperties({ "id", "type", "prompt", "toolName", "inputKey", "outputKey", "branches" })
    public FlowNodeSpec(String id, FlowNodeType type, String prompt, String toolName, String inputKey, String outputKey, Map<String, String> branches) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        branches = branches == null ? Map.of() : Map.copyOf(branches);
        this.id = id;
        this.type = type;
        this.prompt = prompt;
        this.toolName = toolName;
        this.inputKey = inputKey;
        this.outputKey = outputKey;
        this.branches = branches;
    }

    /**
     * 创建构建器。
     *
     * @return 空构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 流式构建器：逐步登记节点各组件。
     */
    public static final class Builder {

        /**
         * 默认构造器（构建器无状态，初始为空）。
         */
        public Builder() {
        }

        private String id;

        private FlowNodeType type;

        private String prompt;

        private String toolName;

        private String inputKey;

        private String outputKey;

        private Map<String, String> branches = Map.of();

        /**
         * 设置节点 id。
         *
         * @param id 节点标识
         * @return 当前构建器
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * 设置节点类型。
         *
         * @param type 节点类型
         * @return 当前构建器
         */
        public Builder type(FlowNodeType type) {
            this.type = type;
            return this;
        }

        /**
         * 设置 LLM 节点提示词。
         *
         * @param prompt 提示词模板
         * @return 当前构建器
         */
        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        /**
         * 设置工具节点目标工具名。
         *
         * @param toolName 工具名
         * @return 当前构建器
         */
        public Builder toolName(String toolName) {
            this.toolName = toolName;
            return this;
        }

        /**
         * 设置读取的 state 字段。
         *
         * @param inputKey 输入字段名
         * @return 当前构建器
         */
        public Builder inputKey(String inputKey) {
            this.inputKey = inputKey;
            return this;
        }

        /**
         * 设置写入的 state 字段。
         *
         * @param outputKey 输出字段名
         * @return 当前构建器
         */
        public Builder outputKey(String outputKey) {
            this.outputKey = outputKey;
            return this;
        }

        /**
         * 设置分支路由表。
         *
         * @param branches state 值 → 下一节点 id
         * @return 当前构建器
         */
        public Builder branches(Map<String, String> branches) {
            this.branches = branches;
            return this;
        }

        /**
         * 构建节点定义。
         *
         * @return 节点定义
         */
        public FlowNodeSpec build() {
            return new FlowNodeSpec(id, type, prompt, toolName, inputKey, outputKey, branches);
        }
    }

    public String id() {
        return id;
    }

    public FlowNodeType type() {
        return type;
    }

    public String prompt() {
        return prompt;
    }

    public String toolName() {
        return toolName;
    }

    public String inputKey() {
        return inputKey;
    }

    public String outputKey() {
        return outputKey;
    }

    public Map<String, String> branches() {
        return branches;
    }

    public String getId() {
        return id();
    }

    public FlowNodeType getType() {
        return type();
    }

    public String getPrompt() {
        return prompt();
    }

    public String getToolName() {
        return toolName();
    }

    public String getInputKey() {
        return inputKey();
    }

    public String getOutputKey() {
        return outputKey();
    }

    public Map<String, String> getBranches() {
        return branches();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        FlowNodeSpec other = (FlowNodeSpec) obj;
        return Objects.equals(this.id, other.id) && Objects.equals(this.type, other.type) && Objects.equals(this.prompt, other.prompt) && Objects.equals(this.toolName, other.toolName) && Objects.equals(this.inputKey, other.inputKey) && Objects.equals(this.outputKey, other.outputKey) && Objects.equals(this.branches, other.branches);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(type);
        result = 31 * result + Objects.hashCode(prompt);
        result = 31 * result + Objects.hashCode(toolName);
        result = 31 * result + Objects.hashCode(inputKey);
        result = 31 * result + Objects.hashCode(outputKey);
        result = 31 * result + Objects.hashCode(branches);
        return result;
    }

    @Override
    public String toString() {
        return "FlowNodeSpec[id=" + id + ", type=" + type + ", prompt=" + prompt + ", toolName=" + toolName + ", inputKey=" + inputKey + ", outputKey=" + outputKey + ", branches=" + branches + "]";
    }
}
