package io.ddd4j.ai.cmpt.chat.service;

import io.ddd4j.ai.cmpt.memory.service.MemoryService;
import io.ddd4j.ai.cmpt.memory.service.impl.WindowMemoryService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ChatService} 端口契约测试：以回声桩验证单轮/多轮/流式语义。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class ChatServiceContractTest {

    /** 回声桩：多轮路径真实读写记忆。 */
    static class EchoChatService implements ChatService {

        final MemoryService memory = new WindowMemoryService(new InMemoryChatMemoryRepository(), 20);
        final List<String> seenInputs = new CopyOnWriteArrayList<>();

        @Override
        public String chat(String message) {
            seenInputs.add(message);
            return "echo:" + message;
        }

        @Override
        public String chat(String message, String conversationId) {
            memory.add(conversationId, List.of(new UserMessage(message)));
            String answer = chat(message);
            memory.add(conversationId, List.of(new AssistantMessage(answer)));
            return answer;
        }

        @Override
        public Flux<String> streamChat(String message) {
            return Flux.just("echo:", message);
        }
    }

    @Test
    void singleTurnReturnsAnswer() {
        assertThat(new EchoChatService().chat("你好")).isEqualTo("echo:你好");
    }

    @Test
    void multiTurnWritesBackToMemory() {
        EchoChatService service = new EchoChatService();

        service.chat("第一问", "conv-1");
        service.chat("第二问", "conv-1");

        assertThat(service.memory.get("conv-1")).hasSize(4); // 2 轮 × (user+assistant)
        assertThat(service.memory.get("conv-1").get(3).getText()).isEqualTo("echo:第二问");
    }

    @Test
    void streamingEmitsIncrementalContent() {
        Flux<String> stream = new EchoChatService().streamChat("你好");

        assertThat(stream.collectList().block()).containsExactly("echo:", "你好");
    }
}
