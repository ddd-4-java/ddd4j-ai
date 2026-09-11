package io.ddd4j.ai.extension.tts.chunk;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ChunkBoundary} 标点判定测试。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class ChunkBoundaryTest {

    @Test
    void chineseSentenceEndPunctuations() {
        assertThat(ChunkBoundary.isBoundary('。')).isTrue();
        assertThat(ChunkBoundary.isBoundary('！')).isTrue();
        assertThat(ChunkBoundary.isBoundary('？')).isTrue();
        assertThat(ChunkBoundary.isBoundary('；')).isTrue();
    }

    @Test
    void englishSentenceEndPunctuations() {
        assertThat(ChunkBoundary.isBoundary('.')).isTrue();
        assertThat(ChunkBoundary.isBoundary('!')).isTrue();
        assertThat(ChunkBoundary.isBoundary('?')).isTrue();
        assertThat(ChunkBoundary.isBoundary(';')).isTrue();
    }

    @Test
    void newlineIsBoundary() {
        assertThat(ChunkBoundary.isBoundary('\n')).isTrue();
    }

    @Test
    void nonBoundaryCharacters() {
        assertThat(ChunkBoundary.isBoundary(',')).isFalse();
        assertThat(ChunkBoundary.isBoundary('，')).isFalse();
        assertThat(ChunkBoundary.isBoundary('a')).isFalse();
        assertThat(ChunkBoundary.isBoundary('中')).isFalse();
        assertThat(ChunkBoundary.isBoundary(' ')).isFalse();
        assertThat(ChunkBoundary.isBoundary('\0')).isFalse();
    }
}