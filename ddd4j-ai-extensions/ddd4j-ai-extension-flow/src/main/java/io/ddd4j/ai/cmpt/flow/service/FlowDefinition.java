package io.ddd4j.ai.cmpt.flow.service;

import java.util.List;
import java.util.Objects;

/**
 * 工作流定义（声明式 DSL）：节点集合 + 有向边集合。
 *
 * @param name   工作流名称（诊断标识）
 * @param nodes  节点定义
 * @param edges  有向边（from → to；from 为 {@code "START"} 表示入口）
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public record FlowDefinition(String name, List<FlowNodeSpec> nodes, List<FlowEdge> edges) {

    public FlowDefinition {
        Objects.requireNonNull(name, "name must not be null");
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String name;
        private final java.util.ArrayList<FlowNodeSpec> nodes = new java.util.ArrayList<>();
        private final java.util.ArrayList<FlowEdge> edges = new java.util.ArrayList<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder node(FlowNodeSpec node) {
            nodes.add(node);
            return this;
        }

        public Builder edge(FlowEdge edge) {
            edges.add(edge);
            return this;
        }

        public FlowDefinition build() {
            return new FlowDefinition(name, nodes, edges);
        }
    }
}
