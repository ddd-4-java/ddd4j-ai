package io.ddd4j.ai.cmpt.router.service.impl;

import io.ddd4j.ai.cmpt.router.service.RoutingStrategy;
import io.ddd4j.ai.core.AiRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MultiModelChatRouter} 单元测试：按策略分发到对应 ChatClient，并回写耗时。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class MultiModelChatRouterTest {

    @Test
    void route_forwardsToSelectedClient() {
        Map<String, ChatClient> clients = new LinkedHashMap<>();
        clients.put("modelA", stubClient("from A"));
        clients.put("modelB", stubClient("from B"));
        MultiModelChatRouter router = new MultiModelChatRouter(clients, new RoundRobinStrategy());

        assertThat(router.route(AiRequest.of("hi"))).isEqualTo("from A");
        assertThat(router.route(AiRequest.of("hi"))).isEqualTo("from B");
        verify(clients.get("modelA")).prompt();
        verify(clients.get("modelB")).prompt();
    }

    @Test
    void route_recordsLatencyToStrategy() {
        Map<String, ChatClient> clients = new LinkedHashMap<>();
        clients.put("modelA", stubClient("from A"));
        RoutingStrategy spy = org.mockito.Mockito.spy(new LatencyStrategy());
        MultiModelChatRouter router = new MultiModelChatRouter(clients, spy);

        router.route(AiRequest.of("hi"));

        org.mockito.Mockito.verify(spy).recordLatency(org.mockito.ArgumentMatchers.eq("modelA"),
                org.mockito.ArgumentMatchers.longThat(l -> l >= 0));
    }

    @Test
    void route_rejectsEmptyClientPool() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> new MultiModelChatRouter(Map.of(), new RoundRobinStrategy()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ChatClient stubClient(String content) {
        ChatClient client = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec spec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);
        when(client.prompt()).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(call);
        when(call.content()).thenReturn(content);
        return client;
    }
}
