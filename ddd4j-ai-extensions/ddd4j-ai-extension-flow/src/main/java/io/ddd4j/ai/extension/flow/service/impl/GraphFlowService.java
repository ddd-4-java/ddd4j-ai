package io.ddd4j.ai.extension.flow.service.impl;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import io.ddd4j.ai.extension.chat.service.ChatService;
import io.ddd4j.ai.extension.flow.service.FlowDefinition;
import io.ddd4j.ai.extension.flow.service.FlowEdge;
import io.ddd4j.ai.extension.flow.service.FlowNodeSpec;
import io.ddd4j.ai.extension.flow.service.FlowNodeType;
import io.ddd4j.ai.extension.flow.service.FlowService;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 图工作流实现：将声明式 DSL 翻译为 Spring AI Alibaba StateGraph 节点与边。
 * <ul>
 *   <li>LLM 节点：prompt（{key} 占位符替换为 state 值）调对话端口，结果写 outputKey</li>
 *   <li>TOOL 节点：以 inputKey 的 state 值为入参执行 ToolCallback，结果写 outputKey</li>
 *   <li>BRANCH 节点：按 inputKey 的 state 值经 branches 映射路由（未命中抛异常）</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class GraphFlowService implements FlowService {

    private final ChatService chatService;
    private final List<ToolCallback> toolCallbacks;
    private final io.ddd4j.ai.extension.agent.service.AgentService agentService;

    /**
     * 构造图工作流服务（不挂智能体端口）。
     *
     * @param chatService   对话端口（LLM 节点执行用）
     * @param toolCallbacks 工具回调列表；{@code null} 按空列表处理
     */
    public GraphFlowService(ChatService chatService, List<ToolCallback> toolCallbacks) {
        this(chatService, toolCallbacks, null);
    }

    /**
     * 构造图工作流服务。
     *
     * @param chatService   对话端口（LLM 节点执行用）
     * @param toolCallbacks 工具回调列表；{@code null} 按空列表处理
     * @param agentService  智能体端口（AGENT 节点执行用）；可为 {@code null}
     */
    public GraphFlowService(ChatService chatService, List<ToolCallback> toolCallbacks,
                            io.ddd4j.ai.extension.agent.service.AgentService agentService) {
        this.chatService = chatService;
        this.toolCallbacks = toolCallbacks == null ? List.of() : List.copyOf(toolCallbacks);
        this.agentService = agentService;
    }

    /**
     * 把声明式 DSL 编译为可执行的 {@link CompiledGraph}：注册节点动作、
     * 条件边（BRANCH 路由）与普通边。
     *
     * @param definition 工作流定义
     * @return 编译后的图实例
     * @throws GraphStateException 图结构非法（如节点缺失、边指向未知节点）时
     */
    @Override
    public CompiledGraph compile(FlowDefinition definition) throws GraphStateException {
        StateGraph graph = new StateGraph();
        for (FlowNodeSpec spec : definition.nodes()) {
            graph.addNode(spec.id(), switch (spec.type()) {
                case LLM -> llmAction(spec);
                case TOOL -> toolAction(spec);
                case AGENT -> agentAction(spec);
                case BRANCH -> noopAction();
            });
        }
        for (FlowNodeSpec spec : definition.nodes()) {
            if (spec.type() == FlowNodeType.BRANCH) {
                Map<String, String> routes = new java.util.HashMap<>();
                spec.branches().forEach((key, target) -> routes.put(key, resolveTarget(target)));
                graph.addConditionalEdges(spec.id(), branchAction(spec), routes);
            }
        }
        for (FlowEdge edge : definition.edges()) {
            graph.addEdge(resolveTarget(edge.from()), resolveTarget(edge.to()));
        }
        return graph.compile();
    }

    /** DSL 的 START/END 字面量映射为图引擎常量（__START__/__END__）。 */
    private static String resolveTarget(String id) {
        if ("START".equals(id)) {
            return StateGraph.START;
        }
        if ("END".equals(id)) {
            return StateGraph.END;
        }
        return id;
    }

    private static AsyncNodeAction noopAction() {
        return state -> CompletableFuture.completedFuture(Map.of());
    }

    /**
     * 同步运行编译后的图：阻塞至最后一个节点，返回终态 state。
     *
     * @param graph 编译后的图
     * @param input 初始 state
     * @return 终态 state 数据；无任何输出时为空 Map
     */
    @Override
    public Map<String, Object> run(CompiledGraph graph, Map<String, Object> input) {
        NodeOutput last = graph.stream(input).blockLast();
        if (last == null) {
            return Map.of();
        }
        return last.state().data();
    }

    /**
     * 流式运行编译后的图：逐节点产出 {@link NodeOutput}。
     *
     * @param graph 编译后的图
     * @param input 初始 state
     * @return 节点输出流
     */
    @Override
    public Flux<NodeOutput> stream(CompiledGraph graph, Map<String, Object> input) {
        return graph.stream(input);
    }

    private AsyncNodeAction llmAction(FlowNodeSpec spec) {
        return state -> CompletableFuture.completedFuture(
                Map.of(spec.outputKey(), chatService.chat(buildPrompt(spec, state), null)));
    }

    private AsyncNodeAction toolAction(FlowNodeSpec spec) {
        return state -> {
            ToolCallback callback = toolCallbacks.stream()
                    .filter(tool -> tool.getToolDefinition().name().equals(spec.toolName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("未知工具: " + spec.toolName()));
            String input = state.value(spec.inputKey()).map(Object::toString).orElse("{}");
            return CompletableFuture.completedFuture(Map.of(spec.outputKey(), callback.call(input)));
        };
    }

    /** AGENT 节点：prompt 作指令调 AgentService（Agentscope HarnessAgent），最终回答写 outputKey。 */
    private AsyncNodeAction agentAction(FlowNodeSpec spec) {
        return state -> {
            if (agentService == null) {
                return CompletableFuture.failedFuture(
                        new IllegalStateException("AgentService 未装配（AGENT 节点不可用）: " + spec.id()));
            }
            String instruction = buildPrompt(spec, state);
            try {
                var result = agentService.execute(io.ddd4j.ai.extension.agent.service.AgentTask.of(instruction));
                return CompletableFuture.completedFuture(Map.of(spec.outputKey(), result.output()));
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        };
    }

    private AsyncEdgeAction branchAction(FlowNodeSpec spec) {
        return state -> CompletableFuture.completedFuture(
                state.value(spec.inputKey()).map(Object::toString).orElse(""));
    }

    private static String buildPrompt(FlowNodeSpec spec, OverAllState state) {
        String prompt = spec.prompt();
        for (Map.Entry<String, Object> entry : state.data().entrySet()) {
            prompt = prompt.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return prompt;
    }
}
