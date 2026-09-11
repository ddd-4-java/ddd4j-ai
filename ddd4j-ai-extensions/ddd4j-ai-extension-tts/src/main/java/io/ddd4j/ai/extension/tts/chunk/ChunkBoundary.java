package io.ddd4j.ai.extension.tts.chunk;

/**
 * 句末标点判定：覆盖中英文常用句末标点，用于 TextChunker 切分阈值。
 *
 * <p>借鉴自 agentscope-cpp 的 TextChunker 切分规则（{@code src/core/audio/text_chunker.h}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public final class ChunkBoundary {

    private ChunkBoundary() {
    }

    /**
     * 是否是句末标点（中英文 + 换行）。
     *
     * @param c 任意字符
     * @return true 表示遇到该字符应当触发切分
     */
    public static boolean isBoundary(char c) {
        return c == '。' || c == '！' || c == '？'
                || c == '.' || c == '!' || c == '?'
                || c == '；' || c == ';'
                || c == '\n';
    }
}