package io.ddd4j.ai.samples.flow;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import io.ddd4j.ai.extension.flow.service.FlowDefinition;
import io.ddd4j.ai.extension.flow.service.FlowService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * flow AI 工作流组件使用示例：声明式 DSL 编译 + 运行。
 *
 * <p>前提：引入 {@code ddd4j-ai-extension-flow}，并依赖 chat 组件（模型 starter）；
 * 节点类型：LLM（prompt 调模型）、TOOL（执行 ToolCallback）、BRANCH（按 state 路由）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Component
public class FlowSample {

    private final FlowService flowService;

    public FlowSample(FlowService flowService) {
        this.flowService = flowService;
    }

    /** 编译并运行工作流，返回最终 state（各节点 outputKey 结果）。 */
    public Map<String, Object> run(FlowDefinition definition, Map<String, Object> input) throws Exception {
        CompiledGraph graph = flowService.compile(definition);
        return flowService.run(graph, input);
    }
}
