package io.ddd4j.ai.extension.flow.service;

import java.beans.ConstructorProperties;

import java.util.Map;
import java.util.Objects;

/**
 * 工作流节点定义。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
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
 * @param id 节点标识（图内唯一）
 * @param type 节点类型
 * @param prompt LLM 节点提示词；{key} 占位符由 state 同名字段替换
 * @param toolName 工具节点目标工具名
 * @param inputKey TOOL/BRANCH 节点读取的 state 字段
 * @param outputKey LLM/TOOL 节点写入结果的 state 字段
 * @param branches BRANCH 节点路由表：state 值 → 下一节点 id
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String id;

        private FlowNodeType type;

        private String prompt;

        private String toolName;

        private String inputKey;

        private String outputKey;

        private Map<String, String> branches = Map.of();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder type(FlowNodeType type) {
            this.type = type;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder toolName(String toolName) {
            this.toolName = toolName;
            return this;
        }

        public Builder inputKey(String inputKey) {
            this.inputKey = inputKey;
            return this;
        }

        public Builder outputKey(String outputKey) {
            this.outputKey = outputKey;
            return this;
        }

        public Builder branches(Map<String, String> branches) {
            this.branches = branches;
            return this;
        }

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
