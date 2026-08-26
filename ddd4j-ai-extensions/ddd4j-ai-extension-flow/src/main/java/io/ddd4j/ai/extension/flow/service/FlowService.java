package io.ddd4j.ai.extension.flow.service;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * AI 工作流编排端口：声明式 DSL 编译为可执行图 + 运行入口。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface FlowService {

    /**
     * 将声明式工作流定义编译为可执行图。
     *
     * @param definition 工作流定义
     * @return 编译后的图（可重复运行）
     * @throws Exception 节点/边非法（重复 id、未知目标等）
     */
    CompiledGraph compile(FlowDefinition definition) throws Exception;

    /**
     * 执行工作流，返回最终 state（各节点 outputKey 写入的结果）。
     *
     * @param graph 编译后的图
     * @param input 初始 state
     * @return 最终 state
     */
    Map<String, Object> run(CompiledGraph graph, Map<String, Object> input);

    /**
     * 流式执行：按节点产出逐步下发。
     *
     * @param graph 编译后的图
     * @param input 初始 state
     * @return 节点输出流
     */
    Flux<NodeOutput> stream(CompiledGraph graph, Map<String, Object> input);
}
