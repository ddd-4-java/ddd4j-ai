package io.ddd4j.ai.extension.flow.service;

import java.beans.ConstructorProperties;

import java.util.Objects;

/**
 * 工作流有向边：{@code from} → {@code to}；{@code from} 可为 {@code START} 表示入口。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */

public final class FlowEdge {

    private static final long serialVersionUID = 0L;

    private final String from;

    private final String to;

    /**
 * 规范构造器：起止节点非空校验。
 *
 * @param from 起始节点 id（或 START）
 * @param to 目标节点 id（或 END）
 * @throws NullPointerException 当 {@code from} 或 {@code to} 为 {@code null} 时
 */

    @ConstructorProperties({ "from", "to" })
    public FlowEdge(String from, String to) {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
        this.from = from;
        this.to = to;
    }

    public String from() {
        return from;
    }

    public String to() {
        return to;
    }

    public String getFrom() {
        return from();
    }

    public String getTo() {
        return to();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        FlowEdge other = (FlowEdge) obj;
        return Objects.equals(this.from, other.from) && Objects.equals(this.to, other.to);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(from);
        result = 31 * result + Objects.hashCode(to);
        return result;
    }

    @Override
    public String toString() {
        return "FlowEdge[from=" + from + ", to=" + to + "]";
    }
}
