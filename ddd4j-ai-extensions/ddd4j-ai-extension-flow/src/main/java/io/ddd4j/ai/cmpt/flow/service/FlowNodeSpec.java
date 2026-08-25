package io.ddd4j.ai.cmpt.flow.service;

import java.util.Map;
import java.util.Objects;

/**
 * 工作流节点定义。
 *
 * @param id            节点标识（图内唯一）
 * @param type          节点类型
 * @param prompt        LLM 节点提示词；{key} 占位符由 state 同名字段替换
 * @param toolName      工具节点目标工具名
 * @param inputKey      TOOL/BRANCH 节点读取的 state 字段
 * @param outputKey     LLM/TOOL 节点写入结果的 state 字段
 * @param branches      BRANCH 节点路由表：state 值 → 下一节点 id
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public record FlowNodeSpec(
        String id,
        FlowNodeType type,
        String prompt,
        String toolName,
        String inputKey,
        String outputKey,
        Map<String, String> branches) {

    public FlowNodeSpec {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        branches = branches == null ? Map.of() : Map.copyOf(branches);
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
}
