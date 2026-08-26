package io.ddd4j.ai.extension.router.service;

import io.ddd4j.ai.core.AiRequest;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ChatRouter} 端口契约测试：路由语义 / 流式 / 异常传播。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class ChatRouterContractTest {

    static class FakeChatRouter implements ChatRouter {

        private final RuntimeException failure;

        FakeChatRouter() {
            this(null);
        }

        FakeChatRouter(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public String route(AiRequest request) {
            if (failure != null) {
                throw failure;
            }
            return "routed";
        }

        @Override
        public Flux<String> streamRoute(AiRequest request) {
            if (failure != null) {
                return Flux.error(failure);
            }
            return Flux.just("routed");
        }
    }

    @Test
    void route_returnsResult() {
        assertThat(new FakeChatRouter().route(AiRequest.of("hi"))).isEqualTo("routed");
    }

    @Test
    void route_propagatesFailure() {
        RuntimeException cause = new IllegalStateException("router down");
        assertThatThrownBy(() -> new FakeChatRouter(cause).route(AiRequest.of("hi")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("router down");
    }

    @Test
    void streamRoute_emitsTokens() {
        List<String> tokens = new FakeChatRouter().streamRoute(AiRequest.of("hi")).collectList().block();
        assertThat(tokens).containsExactly("routed");
    }

    @Test
    void streamRoute_propagatesFailure() {
        RuntimeException cause = new IllegalStateException("router down");
        List<String> tokens = new FakeChatRouter(cause).streamRoute(AiRequest.of("hi"))
                .onErrorResume(e -> Flux.just(e.getMessage())).collectList().block();
        assertThat(tokens).containsExactly("router down");
    }
}
