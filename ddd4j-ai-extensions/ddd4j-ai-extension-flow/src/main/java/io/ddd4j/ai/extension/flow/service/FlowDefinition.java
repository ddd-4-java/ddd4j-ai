package io.ddd4j.ai.extension.flow.service;

import java.util.List;
import java.util.Objects;

/**
 * 工作流定义（声明式 DSL）：节点集合 + 有向边集合。
 *
 * @param name   工作流名称（诊断标识）
 * @param nodes  节点定义
 * @param edges  有向边（from → to；from 为 {@code "START"} 表示入口）
 * @author ddd4j
 */
public record FlowDefinition(String name, List<FlowNodeSpec> nodes, List<FlowEdge> edges) {

    /**
     * 压缩构造器：节点与边拷贝为不可变列表。
     *
     * @throws NullPointerException {@code name}/{@code nodes}/{@code edges} 任一为 {@code null}
     */
    public FlowDefinition {
        Objects.requireNonNull(name, "name must not be null");
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
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
     * 流式构建器：逐步登记名称、节点与边。
     */
    public static final class Builder {

        /**
         * 默认构造器（构建器无状态，初始为空）。
         */
        public Builder() {
        }

        private String name;
        private final java.util.ArrayList<FlowNodeSpec> nodes = new java.util.ArrayList<>();
        private final java.util.ArrayList<FlowEdge> edges = new java.util.ArrayList<>();

        /**
         * 设置工作流名称。
         *
         * @param name 工作流名称
         * @return 当前构建器
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 追加节点。
         *
         * @param node 节点定义
         * @return 当前构建器
         */
        public Builder node(FlowNodeSpec node) {
            nodes.add(node);
            return this;
        }

        /**
         * 追加有向边。
         *
         * @param edge 有向边
         * @return 当前构建器
         */
        public Builder edge(FlowEdge edge) {
            edges.add(edge);
            return this;
        }

        /**
         * 构建工作流定义。
         *
         * @return 不可变的工作流定义
         */
        public FlowDefinition build() {
            return new FlowDefinition(name, nodes, edges);
        }
    }
}
