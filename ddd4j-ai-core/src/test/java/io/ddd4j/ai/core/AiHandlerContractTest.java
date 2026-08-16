package io.ddd4j.ai.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AiHandler} 调用契约测试：以测试桩验证 request → response 往返与稳定命名。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class AiHandlerContractTest {

    /** 最小测试桩：透传输入并标记自身元数据。 */
    static final class EchoHandler implements AiHandler {

        static final String NAME = "test-echo-handler";

        private int invocations;

        @Override
        public String name() {
            return NAME;
        }

        @Override
        public AiResponse handle(AiRequest request) {
            invocations++;
            return new AiResponse(request.input(), Map.of("handler", NAME));
        }
    }

    @Test
    void handlerIsAnAiComponent() {
        EchoHandler handler = new EchoHandler();
        assertThat(handler).isInstanceOf(AiComponent.class);
    }

    @Test
    void nameIsStableAcrossInvocations() {
        EchoHandler handler = new EchoHandler();
        assertThat(handler.name()).isEqualTo(EchoHandler.NAME).isEqualTo(handler.name());
    }

    @Test
    void handleReturnsNormalizedResponse() {
        EchoHandler handler = new EchoHandler();

        AiResponse response = handler.handle(AiRequest.of("ping"));

        assertThat(response.output()).isEqualTo("ping");
        assertThat(response.metadata()).containsEntry("handler", EchoHandler.NAME);
    }

    @Test
    void handleCarriesRequestMetadataThrough() {
        EchoHandler handler = new EchoHandler();
        AiRequest request = new AiRequest("ping", Map.of("traceId", "t-1"));

        AiResponse response = handler.handle(request);

        assertThat(response.metadata()).containsEntry("handler", EchoHandler.NAME);
        assertThat(request.metadata()).containsEntry("traceId", "t-1");
    }

    @Test
    void repeatedHandleCallsAreStatelessPerRequest() {
        EchoHandler handler = new EchoHandler();
        handler.handle(AiRequest.of("a"));
        handler.handle(AiRequest.of("b"));

        assertThat(handler.handle(AiRequest.of("c")).output()).isEqualTo("c");
    }
}
