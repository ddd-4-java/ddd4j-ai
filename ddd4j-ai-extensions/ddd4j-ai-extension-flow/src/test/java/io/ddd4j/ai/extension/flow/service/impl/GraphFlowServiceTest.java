package io.ddd4j.ai.extension.flow.service.impl;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import io.ddd4j.ai.extension.chat.service.ChatService;
import io.ddd4j.ai.extension.flow.service.FlowDefinition;
import io.ddd4j.ai.extension.flow.service.FlowEdge;
import io.ddd4j.ai.extension.flow.service.FlowNodeSpec;
import io.ddd4j.ai.extension.flow.service.FlowNodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link GraphFlowService} 单元测试：真实 StateGraph 执行（fake 对话 + mock 工具，无需 Docker）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class GraphFlowServiceTest {

    private ChatService chatService;
    private ToolCallback upperTool;

    @BeforeEach
    void setUp() {
        chatService = mock(ChatService.class);
        upperTool = mock(ToolCallback.class);
        when(upperTool.getToolDefinition()).thenReturn(ToolDefinition.builder()
                .name("upper")
                .description("upper")
                .inputSchema("{}")
                .build());
        when(upperTool.call(anyString())).thenAnswer(inv -> inv.getArgument(0).toString().toUpperCase());
    }

    @Test
    void llmChain_writesEachNodeOutputToState() throws Exception {
        when(chatService.chat(anyString(), isNull())).thenReturn("first", "second");
        FlowDefinition definition = FlowDefinition.builder()
                .name("chain")
                .node(llm("a", "do A", "a_out"))
                .node(llm("b", "do B", "b_out"))
                .edge(new FlowEdge("START", "a"))
                .edge(new FlowEdge("a", "b"))
                .edge(new FlowEdge("b", "END"))
                .build();
        GraphFlowService service = new GraphFlowService(chatService, List.of());

        CompiledGraph graph = service.compile(definition);
        Map<String, Object> state = service.run(graph, Map.of());

        assertThat(state).containsEntry("a_out", "first").containsEntry("b_out", "second");
    }

    @Test
    void llmNode_promptPlaceholdersReplacedByState() throws Exception {
        when(chatService.chat(anyString(), isNull())).thenReturn("generated");
        FlowDefinition definition = FlowDefinition.builder()
                .name("placeholder")
                .node(llm("a", "写关于 {topic} 的文章", "a_out"))
                .edge(new FlowEdge("START", "a"))
                .edge(new FlowEdge("a", "END"))
                .build();
        GraphFlowService service = new GraphFlowService(chatService, List.of());

        service.run(service.compile(definition), Map.of("topic", "DDD"));

        verify(chatService).chat("写关于 DDD 的文章", null);
    }

    @Test
    void toolNode_executesToolCallbackWithStateValue() throws Exception {
        when(chatService.chat(anyString(), isNull())).thenReturn("hello");
        FlowDefinition definition = FlowDefinition.builder()
                .name("tool")
                .node(llm("a", "produce text", "text"))
                .node(FlowNodeSpec.builder()
                        .id("t")
                        .type(FlowNodeType.TOOL)
                        .toolName("upper")
                        .inputKey("text")
                        .outputKey("upper_out")
                        .build())
                .edge(new FlowEdge("START", "a"))
                .edge(new FlowEdge("a", "t"))
                .edge(new FlowEdge("t", "END"))
                .build();
        GraphFlowService service = new GraphFlowService(chatService, List.of(upperTool));

        Map<String, Object> state = service.run(service.compile(definition), Map.of());

        assertThat(state).containsEntry("upper_out", "HELLO");
        verify(upperTool).call("hello");
    }

    @Test
    void branchNode_routesByStateValue() throws Exception {
        when(chatService.chat(anyString(), isNull())).thenReturn("probe", "on x", "on y");
        FlowDefinition definition = FlowDefinition.builder()
                .name("branch")
                .node(llm("a", "probe", "a_out"))
                .node(FlowNodeSpec.builder()
                        .id("branch")
                        .type(FlowNodeType.BRANCH)
                        .inputKey("route")
                        .branches(Map.of("x", "xNode", "y", "yNode"))
                        .build())
                .node(llm("xNode", "exec x", "x_out"))
                .node(llm("yNode", "exec y", "y_out"))
                .edge(new FlowEdge("START", "a"))
                .edge(new FlowEdge("a", "branch"))
                .edge(new FlowEdge("xNode", "END"))
                .edge(new FlowEdge("yNode", "END"))
                .build();
        GraphFlowService service = new GraphFlowService(chatService, List.of());

        Map<String, Object> state = service.run(service.compile(definition), Map.of("route", "x"));

        assertThat(state).containsEntry("x_out", "on x").doesNotContainKey("y_out");
        verify(chatService, never()).chat("exec y", null);
    }

    @Test
    void agentNode_executesAgentService() throws Exception {
        io.ddd4j.ai.extension.agent.service.AgentService agentService =
                org.mockito.Mockito.mock(io.ddd4j.ai.extension.agent.service.AgentService.class);
        org.mockito.Mockito.when(agentService.execute(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new io.ddd4j.ai.extension.agent.service.AgentResult(
                        "agent final answer", List.of(), null));
        GraphFlowService service = new GraphFlowService(chatService, List.of(), agentService);

        FlowDefinition definition = FlowDefinition.builder()
                .name("agent-flow")
                .node(FlowNodeSpec.builder()
                        .id("a").type(FlowNodeType.AGENT)
                        .prompt("研究 {topic}").outputKey("agent_out")
                        .build())
                .edge(new FlowEdge("START", "a"))
                .edge(new FlowEdge("a", "END"))
                .build();

        Map<String, Object> state = service.run(service.compile(definition), Map.of("topic", "AI"));

        assertThat(state).containsEntry("agent_out", "agent final answer");
    }

    @Test
    void branchNode_unknownValueFails() throws Exception {
        when(chatService.chat(anyString(), isNull())).thenReturn("probe");
        FlowDefinition definition = FlowDefinition.builder()
                .name("branch-miss")
                .node(llm("a", "probe", "a_out"))
                .node(FlowNodeSpec.builder()
                        .id("branch")
                        .type(FlowNodeType.BRANCH)
                        .inputKey("route")
                        .branches(Map.of("x", "xNode"))
                        .build())
                .node(llm("xNode", "exec x", "x_out"))
                .edge(new FlowEdge("START", "a"))
                .edge(new FlowEdge("a", "branch"))
                .edge(new FlowEdge("xNode", "END"))
                .build();
        GraphFlowService service = new GraphFlowService(chatService, List.of());

        CompiledGraph graph = service.compile(definition);

        assertThatThrownBy(() -> service.run(graph, Map.of("route", "nope")))
                .isInstanceOf(RuntimeException.class);
    }

    private static FlowNodeSpec llm(String id, String prompt, String outputKey) {
        return FlowNodeSpec.builder()
                .id(id)
                .type(FlowNodeType.LLM)
                .prompt(prompt)
                .outputKey(outputKey)
                .build();
    }
}
