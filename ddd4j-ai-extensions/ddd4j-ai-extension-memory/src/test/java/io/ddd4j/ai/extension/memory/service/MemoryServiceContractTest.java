package io.ddd4j.ai.extension.memory.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MemoryService} 端口契约测试：以窗口实现验证端口约定的会话隔离、
 * 读写往返、清空与窗口截断语义。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class MemoryServiceContractTest {

    private final MemoryService memory = new io.ddd4j.ai.extension.memory.service.impl.WindowMemoryService(
            new org.springframework.ai.chat.memory.InMemoryChatMemoryRepository(), MemoryService.DEFAULT_WINDOW_SIZE);

    @Test
    void conversationsAreIsolated() {
        memory.add("a", List.of(new UserMessage("你好")));
        memory.add("b", List.of(new UserMessage("hello")));

        assertThat(memory.get("a")).hasSize(1);
        assertThat(memory.get("a").get(0).getText()).isEqualTo("你好");
        assertThat(memory.get("b")).hasSize(1);
        assertThat(memory.get("b").get(0).getText()).isEqualTo("hello");
    }

    @Test
    void addAndRetrieveRoundTrip() {
        Message user = new UserMessage("问题");
        Message assistant = new AssistantMessage("回答");
        memory.add("c", List.of(user, assistant));

        List<Message> history = memory.get("c");

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getText()).isEqualTo("问题");
        assertThat(history.get(1).getText()).isEqualTo("回答");
    }

    @Test
    void clearRemovesOnlyTargetConversation() {
        memory.add("x", List.of(new UserMessage("1")));
        memory.add("y", List.of(new UserMessage("2")));

        memory.clear("x");

        assertThat(memory.get("x")).isEmpty();
        assertThat(memory.get("y")).hasSize(1);
    }

    @Test
    void singleMessageAddUsesDefaultMethod() {
        // ChatMemory 接口的 add(String, Message) default 方法委托给 add(String, List)
        memory.add("single", new UserMessage("only"));

        assertThat(memory.get("single")).hasSize(1);
    }

    @Test
    void windowKeepsMostRecentMessagesOnly() {
        MemoryService small = new io.ddd4j.ai.extension.memory.service.impl.WindowMemoryService(
                new org.springframework.ai.chat.memory.InMemoryChatMemoryRepository(), 3);

        for (int i = 1; i <= 6; i++) {
            small.add("w", List.of(new UserMessage("m" + i)));
        }

        List<Message> window = small.get("w");
        assertThat(window).hasSize(3);
        assertThat(window.get(0).getText()).isEqualTo("m4");
        assertThat(window.get(2).getText()).isEqualTo("m6");
    }
}
