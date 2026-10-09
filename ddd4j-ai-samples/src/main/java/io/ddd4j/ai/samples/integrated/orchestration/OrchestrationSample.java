package io.ddd4j.ai.samples.integrated.orchestration;

import io.ddd4j.ai.extension.agent.service.AgentService;
import io.ddd4j.ai.extension.agent.service.AgentTask;
import io.ddd4j.ai.extension.flow.service.FlowDefinition;
import io.ddd4j.ai.extension.flow.service.FlowEdge;
import io.ddd4j.ai.extension.flow.service.FlowService;
import io.ddd4j.ai.extension.flow.service.FlowNodeSpec;
import io.ddd4j.ai.extension.flow.service.FlowNodeType;
import io.ddd4j.ai.extension.router.service.ChatRouter;
import io.ddd4j.ai.core.AiRequest;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 场景 7：工作流编排（flow + agent + router）。
 * <p>
 * 展示：声明式 DSL 定义多节点工作流（LLM/Tool/Agent/Branch）→ 编译 → 执行。
 */
@Component
public class OrchestrationSample {

    private final FlowService flowService;
    private final AgentService agentService;
    private final ChatRouter chatRouter;

    /**
     * 构造 OrchestrationSample。
     *
     * @param flowService 工作流服务
     * @param agentService 智能体任务服务
     * @param chatRouter 多模型路由器
     */
    public OrchestrationSample(FlowService flowService, AgentService agentService,
                               ChatRouter chatRouter) {
        this.flowService = flowService;
        this.agentService = agentService;
        this.chatRouter = chatRouter;
    }

    /**
     * 顺序工作流：LLM 节点 A → LLM 节点 B（B 的 prompt 引用 A 的结果）。
     *
     * @return 结果键值对
     * @throws Exception 执行过程中出现异常时抛出
     */
    public Map<String, Object> runSequentialFlow() throws Exception {
        FlowDefinition definition = FlowDefinition.builder()
                .name("sequential-pipeline")
                .node(FlowNodeSpec.builder()
                        .id("analyze").type(FlowNodeType.LLM)
                        .prompt("分析以下需求：{input}，列出关键点")
                        .outputKey("analysis")
                        .build())
                .node(FlowNodeSpec.builder()
                        .id("suggest").type(FlowNodeType.LLM)
                        .prompt("基于分析结果：{analysis}，给出具体建议")
                        .outputKey("suggestion")
                        .build())
                .edge(new FlowEdge("START", "analyze"))
                .edge(new FlowEdge("analyze", "suggest"))
                .edge(new FlowEdge("suggest", "END"))
                .build();
        CompiledGraph graph = flowService.compile(definition);
        return flowService.run(graph, Map.of("input", "如何提升系统性能？"));
    }

    /**
     * Agent 节点工作流：LLM 分析 → Agent 执行任务 → LLM 总结。
     *
     * @return 结果键值对
     * @throws Exception 执行过程中出现异常时抛出
     */
    public Map<String, Object> runAgentNodeFlow() throws Exception {
        FlowDefinition definition = FlowDefinition.builder()
                .name("agent-pipeline")
                .node(FlowNodeSpec.builder()
                        .id("plan").type(FlowNodeType.LLM)
                        .prompt("为以下目标制定一个简单计划：{goal}")
                        .outputKey("plan")
                        .build())
                .node(FlowNodeSpec.builder()
                        .id("execute").type(FlowNodeType.AGENT)
                        .prompt("执行计划：{plan}")
                        .outputKey("execution")
                        .build())
                .node(FlowNodeSpec.builder()
                        .id("summarize").type(FlowNodeType.LLM)
                        .prompt("计划：{plan}\n执行结果：{execution}\n\n请总结。")
                        .outputKey("summary")
                        .build())
                .edge(new FlowEdge("START", "plan"))
                .edge(new FlowEdge("plan", "execute"))
                .edge(new FlowEdge("execute", "summarize"))
                .edge(new FlowEdge("summarize", "END"))
                .build();
        CompiledGraph graph = flowService.compile(definition);
        return flowService.run(graph, Map.of("goal", "调研 AI Agent 框架"));
    }

    /**
     * 路由工作流：按策略选择模型执行任务。
     *
     * @param task 任务描述
     * @return 文本结果
     */
    public String routeAndExecute(String task) {
        return chatRouter.route(AiRequest.of(task));
    }

    /**
     * Agent 直接执行（绕过 workflow，单次任务）。
     *
     * @param instruction 指令内容
     * @return 文本结果
     * @throws Exception 执行过程中出现异常时抛出
     */
    public String agentDirect(String instruction) throws Exception {
        return agentService.execute(AgentTask.of(instruction)).output();
    }
}
