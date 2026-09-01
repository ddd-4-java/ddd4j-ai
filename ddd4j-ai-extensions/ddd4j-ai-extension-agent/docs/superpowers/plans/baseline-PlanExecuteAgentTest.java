package io.ddd4j.ai.extension.agent.service.impl;

import io.ddd4j.ai.extension.agent.service.AgentResult;
import io.ddd4j.ai.extension.agent.service.AgentStep;
import io.ddd4j.ai.extension.agent.service.AgentTask;
import io.ddd4j.ai.extension.chat.service.ChatService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link PlanExecuteAgent} 单元测试：计划解析 / 分步执行聚合 / 无计划兜底。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class PlanExecuteAgentTest {

    @Test
    void execute_plansAndExecutesSteps() throws Exception {
        ChatService chatService = mock(ChatService.class);
        when(chatService.chat(anyString(), any())).thenReturn(
                "1. step one\n2. step two",
                "result one",
                "result two");
        PlanExecuteAgent agent = new PlanExecuteAgent(chatService, 5);

        AgentResult result = agent.execute(AgentTask.of("goal"));

        assertThat(result.output()).isEqualTo("result one\nresult two");
        assertThat(result.steps()).extracting(AgentStep::type)
                .containsExactly("plan", "execution", "execution", "result");
    }

    @Test
    void execute_respectsMaxPlanSteps() throws Exception {
        ChatService chatService = mock(ChatService.class);
        when(chatService.chat(anyString(), any())).thenReturn(
                "1. one\n2. two\n3. three\n4. four",
                "r1",
                "r2");
        PlanExecuteAgent agent = new PlanExecuteAgent(chatService, 2);

        AgentResult result = agent.execute(AgentTask.of("goal"));

        assertThat(result.steps()).filteredOn(step -> "execution".equals(step.type())).hasSize(2);
    }

    @Test
    void execute_noPlanItems_fallsBackToPlanText() throws Exception {
        ChatService chatService = mock(ChatService.class);
        when(chatService.chat(anyString(), any())).thenReturn("直接回答");
        PlanExecuteAgent agent = new PlanExecuteAgent(chatService, 5);

        AgentResult result = agent.execute(AgentTask.of("goal"));

        assertThat(result.output()).isEqualTo("直接回答");
        assertThat(result.steps()).extracting(AgentStep::type).containsExactly("plan", "result");
    }
}
