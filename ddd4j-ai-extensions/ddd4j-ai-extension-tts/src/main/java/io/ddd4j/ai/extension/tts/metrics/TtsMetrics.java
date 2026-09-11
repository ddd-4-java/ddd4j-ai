package io.ddd4j.ai.extension.tts.metrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * TTFA（Time-to-First-Audio）自维护采样器：累积所有合成实例的首块延迟，
 * 提供 samples / avg / min / max 快照与 Logger 摘要输出。
 *
 * <p>借鉴自 agentscope-cpp 的 TTS 评估设计（{@code tts-tech-decision.md} 第 1.2 节
 * 把 TTFA 列为「对话式 AI 最关键的指标」）。
 *
 * <p>不接 Micrometer / actuator：ddd4j-ai 当前零 metrics 依赖，业务方需要 Prometheus/OTEL
 * 时自行注入埋点。
 *
 * <p>线程安全：所有字段均为 {@link AtomicLong}，多线程并发安全。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public final class TtsMetrics {

    private final AtomicLong samples = new AtomicLong();
    private final AtomicLong totalNanos = new AtomicLong();
    private final AtomicLong minNanos = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxNanos = new AtomicLong();

    /**
     * 记录一次首块延迟（自业务发起 appendText / synthesize 起到收到第一个音频字节的耗时）。
     *
     * @param elapsedNanos 耗时（纳秒），传 0 或负数视为无效（不计入）
     */
    public void recordTtfa(long elapsedNanos) {
        if (elapsedNanos <= 0) {
            return;
        }
        samples.incrementAndGet();
        totalNanos.addAndGet(elapsedNanos);
        minNanos.updateAndGet(prev -> Math.min(prev, elapsedNanos));
        maxNanos.updateAndGet(prev -> Math.max(prev, elapsedNanos));
    }

    public long sampleCount() {
        return samples.get();
    }

    public long minNanos() {
        long m = minNanos.get();
        return m == Long.MAX_VALUE ? 0 : m;
    }

    public long maxNanos() {
        return maxNanos.get();
    }

    public long avgNanos() {
        long n = samples.get();
        if (n == 0) {
            return 0;
        }
        return totalNanos.get() / n;
    }

    /**
     * 输出当前统计摘要（建议每 N 次合成或定时调用）。
     *
     * <p>格式：{@code tts.firstaudio: samples=42, avg=97ms, min=80ms, max=210ms}
     *
     * @param log SLF4J logger
     */
    public void logSummary(org.slf4j.Logger log) {
        long n = samples.get();
        if (n == 0) {
            log.info("tts.firstaudio: no samples");
            return;
        }
        long avgMs = avgNanos() / 1_000_000L;
        long minMs = minNanos() / 1_000_000L;
        long maxMs = maxNanos() / 1_000_000L;
        log.info("tts.firstaudio: samples={}, avg={}ms, min={}ms, max={}ms", n, avgMs, minMs, maxMs);
    }

    /** 重置所有统计（用于业务方手动触发新窗口）。 */
    public void reset() {
        samples.set(0);
        totalNanos.set(0);
        minNanos.set(Long.MAX_VALUE);
        maxNanos.set(0);
    }
}