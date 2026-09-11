package io.ddd4j.ai.extension.tts.metrics;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TtsMetrics} TTFA 采样器测试。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class TtsMetricsTest {

    @Test
    void emptyByDefault() {
        TtsMetrics metrics = new TtsMetrics();
        assertThat(metrics.sampleCount()).isZero();
        assertThat(metrics.minNanos()).isZero();
        assertThat(metrics.maxNanos()).isZero();
        assertThat(metrics.avgNanos()).isZero();
    }

    @Test
    void recordsValidSamplesAndComputesAggregates() {
        TtsMetrics metrics = new TtsMetrics();
        metrics.recordTtfa(80_000_000L);   // 80 ms
        metrics.recordTtfa(120_000_000L);  // 120 ms
        metrics.recordTtfa(100_000_000L);  // 100 ms

        assertThat(metrics.sampleCount()).isEqualTo(3);
        assertThat(metrics.minNanos()).isEqualTo(80_000_000L);
        assertThat(metrics.maxNanos()).isEqualTo(120_000_000L);
        assertThat(metrics.avgNanos()).isEqualTo(100_000_000L);
    }

    @Test
    void ignoresNonPositiveSamples() {
        TtsMetrics metrics = new TtsMetrics();
        metrics.recordTtfa(0L);
        metrics.recordTtfa(-1L);
        assertThat(metrics.sampleCount()).isZero();
        assertThat(metrics.avgNanos()).isZero();
    }

    @Test
    void logSummaryFormatsExpectedLine() {
        TtsMetrics metrics = new TtsMetrics();
        org.slf4j.Logger log = LoggerFactory.getLogger(TtsMetricsTest.class);

        metrics.recordTtfa(97_000_000L);   // 97 ms
        metrics.recordTtfa(105_000_000L);  // 105 ms

        // 仅断言不抛异常 + 不影响统计（logSummary 是只读 IO）
        metrics.logSummary(log);
        assertThat(metrics.sampleCount()).isEqualTo(2);
    }

    @Test
    void logSummaryEmitsEmptyWhenNoSamples() {
        TtsMetrics metrics = new TtsMetrics();
        org.slf4j.Logger log = LoggerFactory.getLogger(TtsMetricsTest.class);
        metrics.logSummary(log); // 不应抛
    }

    @Test
    void resetClearsAllCounters() {
        TtsMetrics metrics = new TtsMetrics();
        metrics.recordTtfa(100_000_000L);
        metrics.recordTtfa(200_000_000L);
        metrics.reset();
        assertThat(metrics.sampleCount()).isZero();
        assertThat(metrics.minNanos()).isZero();
        assertThat(metrics.maxNanos()).isZero();
        assertThat(metrics.avgNanos()).isZero();
    }

    @Test
    void concurrentRecordIsThreadSafe() throws InterruptedException {
        TtsMetrics metrics = new TtsMetrics();
        int threads = 8;
        int perThread = 1000;
        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            workers[t] = new Thread(() -> {
                for (int i = 0; i < perThread; i++) {
                    metrics.recordTtfa(50_000_000L + i);
                }
            });
            workers[t].start();
        }
        for (Thread worker : workers) {
            worker.join();
        }
        assertThat(metrics.sampleCount()).isEqualTo((long) threads * perThread);
        assertThat(metrics.minNanos()).isEqualTo(50_000_000L);
        assertThat(metrics.maxNanos()).isEqualTo(50_000_000L + perThread - 1);
    }
}