package io.ddd4j.ai.extension.tts.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文本转语音组件配置（前缀 {@code ddd4j.ai.tts}）。
 *
 * <p>完整配置示例：
 * <pre>{@code
 * ddd4j:
 *   ai:
 *     tts:
 *       enabled: true
 *       primary: dashscope       # 主后端；可选 dashscope / edge
 *       edge:
 *         enabled: true
 *         default-voice: zh-CN-XiaoxiaoNeural
 *       dashscope:
 *         enabled: true
 *         api-key: ${DASHSCOPE_API_KEY}
 *         model: qwen3-tts-12hz-0.6b-customvoice
 *         voice: Cherry
 *       chunker:
 *         max-chars: 24
 *         min-chars: 1
 *       metrics:
 *         enabled: true
 * }</pre>
 *
 * @author ddd4j
 */
@Getter
@Setter
@ConfigurationProperties(prefix = TtsProperties.PREFIX)
public class TtsProperties {

    /**
     * 默认构造器（配置属性类由 Spring 绑定）。
     */
    public TtsProperties() {
    }

    /** 配置属性前缀。 */
    public static final String PREFIX = "ddd4j.ai.tts";

    /** 是否启用 TTS 组件自动装配。 */
    private boolean enabled = true;

    /**
     * 主后端选择：{@code dashscope}（高性能，需 API key）/ {@code edge}（免费兜底）。
     * 当主后端不可用时，自动降级到另一个已启用的后端。
     */
    private String primary = "edge";

    /** Edge TTS 后端配置。 */
    private Edge edge = new Edge();

    /** DashScope Qwen3-TTS Realtime 后端配置。 */
    private DashScope dashscope = new DashScope();

    /** 文本分块配置（与 agentscope-cpp 的 TextChunker 对齐）。 */
    private Chunker chunker = new Chunker();

    /** TTFA 指标配置。 */
    private Metrics metrics = new Metrics();

    /**
     * 解析当前主后端类型（字符串解析失败时回退默认 EDGE）。
     *
     * @return 主后端类型枚举
     */
    public PrimaryType resolvedPrimary() {
        if (primary == null || primary.isBlank()) {
            return PrimaryType.EDGE;
        }
        return PrimaryType.fromString(primary.trim().toLowerCase());
    }

    /**
     * Edge TTS 后端配置。
     */
    @Getter
    @Setter
    public static class Edge {

        /**
         * 默认构造器（配置属性类由 Spring 绑定）。
         */
        public Edge() {
        }

        /** 是否启用 Edge 后端（默认 true）。 */
        private boolean enabled = true;
        /** 默认音色 shortName（Edge TTS 音色，如 zh-CN-XiaoxiaoNeural）。 */
        private String defaultVoice = "zh-CN-XiaoxiaoNeural";
    }

    /**
     * DashScope Qwen3-TTS Realtime 后端配置。
     */
    @Getter
    @Setter
    public static class DashScope {

        /**
         * 默认构造器（配置属性类由 Spring 绑定）。
         */
        public DashScope() {
        }

        /** 是否启用 DashScope 后端（默认 true；无 api-key 时实际不会为不启用 Bean）。 */
        private boolean enabled = true;
        /** DashScope 控制台申请的 API Key。 */
        private String apiKey;
        /** TTS 模型名。 */
        private String model = "qwen3-tts-12hz-0.6b-customvoice";
        /** 默认音色 shortName（如 Cherry）；null 用模型默认。 */
        private String voice = "Cherry";
        /** WebSocket 连接超时（秒）。 */
        private int connectTimeoutSeconds = 10;
    }

    /**
     * 文本分块配置。
     */
    @Getter
    @Setter
    public static class Chunker {

        /**
         * 默认构造器（配置属性类由 Spring 绑定）。
         */
        public Chunker() {
        }

        /** 最大 chunk 字符数（达到此长度强制切分，默认 24 与 agentscope-cpp 对齐）。 */
        private int maxChars = 24;
        /** 最小 chunk 字符数（小于此长度不切，避免太碎）。 */
        private int minChars = 1;
    }

    /**
     * TTFA 指标配置。
     */
    @Getter
    @Setter
    public static class Metrics {

        /**
         * 默认构造器（配置属性类由 Spring 绑定）。
         */
        public Metrics() {
        }

        /** 是否启用 TTFA 采样与 Logger 输出。 */
        private boolean enabled = true;
    }

    /**
     * 主后端类型枚举。
     */
    public enum PrimaryType {
        /** 免费在线 Edge 后端。 */
        EDGE,
        /** DashScope 高性能后端。 */
        DASHSCOPE;

        /**
         * 按名称解析枚举，无法识别时回退 {@link #EDGE}。
         *
         * @param s 后端名称（大小写不敏感）
         * @return 解析出的枚举，识别失败为 {@link #EDGE}
         */
        public static PrimaryType fromString(String s) {
            try {
                return PrimaryType.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return EDGE;
            }
        }
    }
}