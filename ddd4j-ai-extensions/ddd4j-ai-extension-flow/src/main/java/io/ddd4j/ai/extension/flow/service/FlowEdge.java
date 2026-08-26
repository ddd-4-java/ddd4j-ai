package io.ddd4j.ai.extension.flow.service;

import java.util.Objects;

/**
 * 工作流有向边：{@code from} → {@code to}；{@code from} 可为 {@code START} 表示入口。
 *
 * @param from 起始节点 id（或 START）
 * @param to   目标节点 id（或 END）
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public record FlowEdge(String from, String to) {

    public FlowEdge {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
    }
}
