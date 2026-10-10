package io.ddd4j.ai.extension.flow.service;

import java.beans.ConstructorProperties;

import java.util.List;
import java.util.Objects;

/**
 * 工作流定义（声明式 DSL）：节点集合 + 有向边集合。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */

public final class FlowDefinition {

    private static final long serialVersionUID = 0L;

    private final String name;

    private final List<FlowNodeSpec> nodes;

    private final List<FlowEdge> edges;

    /**
 * @param name 工作流名称（诊断标识）
 * @param nodes 节点定义
 * @param edges 有向边（from → to；from 为 {@code "START"} 表示入口）
 */

    @ConstructorProperties({ "name", "nodes", "edges" })
    public FlowDefinition(String name, List<FlowNodeSpec> nodes, List<FlowEdge> edges) {
        Objects.requireNonNull(name, "name must not be null");
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
        this.name = name;
        this.nodes = nodes;
        this.edges = edges;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final java.util.ArrayList<FlowNodeSpec> nodes = new java.util.ArrayList<>();

        private final java.util.ArrayList<FlowEdge> edges = new java.util.ArrayList<>();

        private String name;

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

    public String name() {
        return name;
    }

    public List<FlowNodeSpec> nodes() {
        return nodes;
    }

    public List<FlowEdge> edges() {
        return edges;
    }

    public String getName() {
        return name();
    }

    public List<FlowNodeSpec> getNodes() {
        return nodes();
    }

    public List<FlowEdge> getEdges() {
        return edges();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        FlowDefinition other = (FlowDefinition) obj;
        return Objects.equals(this.name, other.name) && Objects.equals(this.nodes, other.nodes) && Objects.equals(this.edges, other.edges);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(name);
        result = 31 * result + Objects.hashCode(nodes);
        result = 31 * result + Objects.hashCode(edges);
        return result;
    }

    @Override
    public String toString() {
        return "FlowDefinition[name=" + name + ", nodes=" + nodes + ", edges=" + edges + "]";
    }
}
