package io.ddd4j.ai.cmpt.chat.service.impl;

import io.ddd4j.ai.cmpt.chat.service.ChatService;
import io.ddd4j.ai.cmpt.memory.service.MemoryService;
import io.ddd4j.ai.cmpt.memory.service.impl.WindowMemoryService;
import io.ddd4j.ai.core.AiRequest;
import io.ddd4j.ai.core.AiResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ChatClientAdapter} 单元测试：mock ChatClient 链式调用，
 * 验证单轮转发、多轮记忆读写、AiHandler 直通、流式与缺失记忆的错误。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class ChatClientAdapterTest {

    private final ChatClient chatClient = mock(ChatClient.class);
    private final ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
    private final ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);
    private final ChatClient.StreamResponseSpec streamSpec = mock(ChatClient.StreamResponseSpec.class);

    @SuppressWarnings("unchecked")
    private void stubSingleTurn(String answer) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(answer);
    }

    @Test
    void singleTurnForwardsToChatClient() {
        stubSingleTurn("回答");

        String answer = new ChatClientAdapter(chatClient, null, null).chat("问题");

        assertThat(answer).isEqualTo("回答");
    }

    @Test
    void systemPromptIsAppliedWhenConfigured() {
        stubSingleTurn("回答");

        new ChatClientAdapter(chatClient, null, "你是助手").chat("问题");

        org.mockito.Mockito.verify(requestSpec).system("你是助手");
    }

    @Test
    void multiTurnCarriesHistoryAndWritesBack() {
        // 多轮走 prompt(Prompt) 分支
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("第二答");

        MemoryService memory = new WindowMemoryService(new InMemoryChatMemoryRepository(), 20);
        memory.add("c1", List.of(new UserMessage("第一问"), new AssistantMessage("第一答")));

        String answer = new ChatClientAdapter(chatClient, memory, null).chat("第二问", "c1");

        assertThat(answer).isEqualTo("第二答");
        // 历史被带入 Prompt（2 条历史 + 1 条新消息）
        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        org.mockito.Mockito.verify(chatClient).prompt(captor.capture());
        assertThat(captor.getValue().getInstructions()).hasSize(3);
        // 本轮问答写回记忆
        assertThat(memory.get("c1")).hasSize(4);
    }

    @Test
    void multiTurnWithoutMemoryFailsFast() {
        stubSingleTurn("回答");

        assertThrows(IllegalStateException.class,
                () -> new ChatClientAdapter(chatClient, null, null).chat("问题", "c1"));
    }

    @Test
    void streamingReturnsContentFlux() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.content()).thenReturn(Flux.just("你", "好"));

        ChatService service = new ChatClientAdapter(chatClient, null, null);

        assertThat(service.streamChat("问题").collectList().block()).containsExactly("你", "好");
    }

    @Test
    void aiHandlerDelegatesToChat() {
        stubSingleTurn("回答");

        AiResponse response = new ChatClientAdapter(chatClient, null, null)
                .handle(AiRequest.of("问题"));

        assertThat(response.output()).isEqualTo("回答");
    }

    @Test
    void nameIsStable() {
        assertThat(new ChatClientAdapter(chatClient, null, null).name())
                .isEqualTo(ChatClientAdapter.NAME)
                .isEqualTo("ddd4j-ai-chat");
    }
}
