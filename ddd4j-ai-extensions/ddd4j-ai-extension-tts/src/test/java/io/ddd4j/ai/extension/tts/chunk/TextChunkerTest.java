package io.ddd4j.ai.extension.tts.chunk;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TextChunker} 流式文本切分测试：覆盖标点边界 / 长度阈值 / reset / flush 等行为。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class TextChunkerTest {

    @Test
    void rejectsInvalidConfig() {
        assertThatThrownBy(() -> TextChunker.create(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxChars");
        assertThatThrownBy(() -> TextChunker.create(10, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minChars");
    }

    @Test
    void emptyOrNullDeltaProducesNoChunks() {
        TextChunker chunker = TextChunker.defaultChunker();
        assertThat(chunker.feed(null)).isEmpty();
        assertThat(chunker.feed("")).isEmpty();
        assertThat(chunker.pendingSize()).isZero();
    }

    @Test
    void chineseSentenceBoundaryTriggersFlush() {
        // maxChars=24, minChars=1: 一句中文（6 字 + 标点）应当触发切分
        TextChunker chunker = TextChunker.create(24, 1);
        List<String> chunks = chunker.feed("你好世界！");
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo("你好世界！");
        assertThat(chunker.pendingSize()).isZero();
    }

    @Test
    void englishSentenceBoundaryTriggersFlush() {
        TextChunker chunker = TextChunker.create(24, 1);
        List<String> chunks = chunker.feed("Hello world.");
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo("Hello world.");
    }

    @Test
    void mixedPunctuationsAndNewline() {
        TextChunker chunker = TextChunker.create(24, 1);
        assertThat(chunker.feed("foo;")).containsExactly("foo;");
        assertThat(chunker.feed("bar\n")).containsExactly("bar\n");
        assertThat(chunker.feed("baz?")).containsExactly("baz?");
    }

    @Test
    void maxCharsTriggersForceSplit() {
        // 24 字符正好不切；25 字符切前 24
        TextChunker chunker = TextChunker.create(24, 1);
        String input = "1234567890123456789012345"; // 25 个数字
        List<String> chunks = chunker.feed(input);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).hasSize(24);
        assertThat(chunker.pendingSize()).isEqualTo(1);
    }

    @Test
    void maxCharsWithTrailingBoundaryInMultipleChunks() {
        // maxChars=4: 喂入 "abc。" 应切出 "abc。"（句末标点优先于 maxChars 切分）
        TextChunker chunker = TextChunker.create(4, 1);
        List<String> chunks = chunker.feed("abc。");
        assertThat(chunks).containsExactly("abc。");
        assertThat(chunker.pendingSize()).isZero();
    }

    @Test
    void incrementalDeltasAccumulateThenFlush() {
        // 模拟 LLM 流式输出："你" → "好" → "世" → "界" → "！"
        TextChunker chunker = TextChunker.create(24, 1);
        assertThat(chunker.feed("你")).isEmpty();
        assertThat(chunker.feed("好")).isEmpty();
        assertThat(chunker.feed("世")).isEmpty();
        assertThat(chunker.feed("界")).isEmpty();
        assertThat(chunker.feed("！")).containsExactly("你好世界！");
        assertThat(chunker.pendingSize()).isZero();
    }

    @Test
    void flushReturnsRemainingBuffer() {
        TextChunker chunker = TextChunker.defaultChunker();
        chunker.feed("未完待续");
        assertThat(chunker.flush()).isEqualTo("未完待续");
        assertThat(chunker.flush()).isEmpty();
    }

    @Test
    void resetClearsBuffer() {
        TextChunker chunker = TextChunker.defaultChunker();
        chunker.feed("abc");
        chunker.reset();
        assertThat(chunker.pendingSize()).isZero();
        assertThat(chunker.flush()).isEmpty();
    }

    @Test
    void maxCharsForcesMultipleChunksAcrossFeeds() {
        // maxChars=4: 喂 "abcd" 应切出前 4，剩余 buf 为空；再喂 "ef" 不切（无标点且 < minChars*1）
        TextChunker chunker = TextChunker.create(4, 1);
        assertThat(chunker.feed("abcd")).containsExactly("abcd");
        assertThat(chunker.pendingSize()).isZero();
        assertThat(chunker.feed("ef")).isEmpty();
        assertThat(chunker.flush()).isEqualTo("ef");
    }

    @Test
    void minCharsPreventsFragmentingShortPrefix() {
        // maxChars=24, minChars=5: 喂 "abc" 不切（< minChars），喂 "def" 凑到 6 后末尾无标点 → 不切
        TextChunker chunker = TextChunker.create(24, 5);
        assertThat(chunker.feed("abc")).isEmpty();
        assertThat(chunker.feed("def")).isEmpty();
        assertThat(chunker.pendingSize()).isEqualTo(6);
        assertThat(chunker.flush()).isEqualTo("abcdef");
    }
}