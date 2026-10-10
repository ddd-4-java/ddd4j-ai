package io.ddd4j.ai.extension.flow.service;

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

    /**
     * 规范构造器：id/type 非空校验，路由表 {@code null} 归一为空 Map。
     *
     * @param id        节点标识（图内唯一）
     * @param type      节点类型
     * @param prompt    LLM 节点提示词；{key} 占位符由 state 同名字段替换
     * @param toolName  工具节点目标工具名
     * @param inputKey  TOOL/BRANCH 节点读取的 state 字段
     * @param outputKey LLM/TOOL 节点写入结果的 state 字段
     * @param branches  BRANCH 节点路由表：state 值 → 下一节点 id
     * @throws NullPointerException 当 {@code id} 或 {@code type} 为 {@code null} 时
     */
    public FlowNodeSpec {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        branches = branches == null ? Map.of() : Map.copyOf(branches);
    }

    /**
     * 创建流式构建器。
     *
     * @return 空的 {@link Builder} 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 工作流节点的流式构建器（链式设置 id、类型、提示词、工具与路由）。
     */
    public static final class Builder {

        /**
         * 构造 Builder 对象。
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
         * 设置节点标识。
         *
         * @param id 节点标识（图内唯一）
         * @return 当前构建器（链式调用）
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * 设置节点类型。
         *
         * @param type 节点类型
         * @return 当前构建器（链式调用）
         */
        public Builder type(FlowNodeType type) {
            this.type = type;
            return this;
        }

        /**
         * 设置 LLM 节点提示词。
         *
         * @param prompt 提示词（{key} 占位符由 state 同名字段替换）
         * @return 当前构建器（链式调用）
         */
        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        /**
         * 设置工具节点目标工具名。
         *
         * @param toolName 工具名
         * @return 当前构建器（链式调用）
         */
        public Builder toolName(String toolName) {
            this.toolName = toolName;
            return this;
        }

        /**
         * 设置节点读取的 state 字段。
         *
         * @param inputKey state 字段名
         * @return 当前构建器（链式调用）
         */
        public Builder inputKey(String inputKey) {
            this.inputKey = inputKey;
            return this;
        }

        /**
         * 设置节点写入结果的 state 字段。
         *
         * @param outputKey state 字段名
         * @return 当前构建器（链式调用）
         */
        public Builder outputKey(String outputKey) {
            this.outputKey = outputKey;
            return this;
        }

        /**
         * 设置 BRANCH 节点路由表。
         *
         * @param branches state 值 → 下一节点 id 的映射
         * @return 当前构建器（链式调用）
         */
        public Builder branches(Map<String, String> branches) {
            this.branches = branches;
            return this;
        }

        /**
         * 构建不可变的节点定义。
         *
         * @return {@link FlowNodeSpec} 实例
         */
        public FlowNodeSpec build() {
            return new FlowNodeSpec(id, type, prompt, toolName, inputKey, outputKey, branches);
        }
    }
}
