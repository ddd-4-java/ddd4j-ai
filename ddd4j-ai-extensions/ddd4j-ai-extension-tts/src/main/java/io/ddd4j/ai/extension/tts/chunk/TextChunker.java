package io.ddd4j.ai.extension.tts.chunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 流式文本分块器：把 LLM 累积的文本按标点边界 / 长度上限切成 TTS 友好的 chunk。
 *
 * <p>设计借鉴自 agentscope-cpp 的
 * {@code include/agentscope/core/event/text_chunker.h}（StreamFirstChunkMiddleware 的核心组件）。
 *
 * <p>切分规则：
 * <ul>
 *   <li>缓冲文本达到 {@code maxChars} → 强制切分</li>
 *   <li>缓冲文本末尾出现句末标点（{@link ChunkBoundary#isBoundary(char)}）→ 在标点后切分</li>
 *   <li>调用 {@link #flush()} → 强制 flush 残余</li>
 * </ul>
 *
 * <p>使用示例（对应 agentscope-cpp 的 StreamFirstChunkMiddleware 用法）：
 * <pre>{@code
 * TextChunker chunker = TextChunker.create(24, 1);
 * for (String delta : textDeltas) {
 *     for (String chunk : chunker.feed(delta)) {
 *         ttsService.streamSynthesize(chunk, voice).subscribe();
 *     }
 * }
 * String tail = chunker.flush();
 * if (!tail.isEmpty()) ttsService.streamSynthesize(tail, voice).subscribe();
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public final class TextChunker {

    /** Qwen3-TTS 单字符流式推荐上限（与 agentscope-cpp 默认值对齐）。 */
    public static final int DEFAULT_MAX_CHARS = 24;

    /** 最小切分阈值（小于此长度不切，避免太碎）。 */
    public static final int DEFAULT_MIN_CHARS = 1;

    private final int maxChars;
    private final int minChars;
    private final StringBuilder buffer;

    private TextChunker(int maxChars, int minChars) {
        if (maxChars < 1) {
            throw new IllegalArgumentException("maxChars must be >= 1, got: " + maxChars);
        }
        if (minChars < 0 || minChars > maxChars) {
            throw new IllegalArgumentException(
                    "minChars must be in [0, maxChars], got: " + minChars + " / " + maxChars);
        }
        this.maxChars = maxChars;
        this.minChars = minChars;
        this.buffer = new StringBuilder();
    }

    public static TextChunker create(int maxChars, int minChars) {
        return new TextChunker(maxChars, minChars);
    }

    public static TextChunker defaultChunker() {
        return new TextChunker(DEFAULT_MAX_CHARS, DEFAULT_MIN_CHARS);
    }

    /**
     * 喂入一段新文本增量，返回需要立即 flush 的 chunks（可能为空列表）。
     *
     * <p>返回的 chunk 已经从内部缓冲移除；调用方处理后下次 feed 继续累积。
     *
     * @param delta 新到达的文本片段（{@code null} 视为空字符串）
     * @return 需要立即送 TTS 的 chunks 列表（按时间顺序）
     */
    public List<String> feed(String delta) {
        if (delta == null || delta.isEmpty()) {
            return Collections.emptyList();
        }
        buffer.append(delta);

        List<String> chunks = new ArrayList<>(2);
        while (true) {
            int len = buffer.length();
            if (len < Math.max(minChars, 1)) {
                break;
            }
            // 1) 强制切分（达到 maxChars）
            if (len >= maxChars) {
                chunks.add(buffer.substring(0, maxChars));
                buffer.delete(0, maxChars);
                continue;
            }
            // 2) 句末标点切分（len 在 [minChars, maxChars) 区间）
            int lastBoundary = -1;
            for (int i = len - 1; i >= minChars; i--) {
                if (ChunkBoundary.isBoundary(buffer.charAt(i))) {
                    lastBoundary = i;
                    break;
                }
            }
            if (lastBoundary >= 0) {
                chunks.add(buffer.substring(0, lastBoundary + 1));
                buffer.delete(0, lastBoundary + 1);
                continue;
            }
            // 无切分条件，继续累积
            break;
        }
        return chunks;
    }

    /**
     * 强制 flush 剩余缓冲（流结束时调用）。
     *
     * @return 剩余文本（可能为空字符串）
     */
    public String flush() {
        if (buffer.isEmpty()) {
            return "";
        }
        String remaining = buffer.toString();
        buffer.setLength(0);
        return remaining;
    }

    /** 当前缓冲长度（用于监控 / 调试）。 */
    public int pendingSize() {
        return buffer.length();
    }

    /** 重置内部缓冲（重新开始累积，例如每轮新对话前）。 */
    public void reset() {
        buffer.setLength(0);
    }
}