package io.ddd4j.ai.samples.tts;

import io.ddd4j.ai.extension.tts.service.TtsService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * tts 文本转语音组件使用示例：Edge TTS 免费在线合成（无需密钥）。
 *
 * <p>前提：引入 {@code ddd4j-ai-extension-tts}，默认音色
 * {@code ddd4j.ai.tts.default-voice=zh-CN-XiaoxiaoNeural}，需外网访问 Edge 服务。
 *
 * @author ddd4j
 */
@Component
public class TtsSample {

    private final TtsService ttsService;

    /**
     * 构造 TtsSample。
     *
     * @param ttsService TTS 语音合成服务
     */
    public TtsSample(TtsService ttsService) {
        this.ttsService = ttsService;
    }

    /**
     * 合成完整 mp3 音频字节。
     *
     * @param text 文本内容
     * @return 执行结果
     * @throws Exception 执行过程中出现异常时抛出
     */
    public byte[] speak(String text) throws Exception {
        return ttsService.synthesize(text, null);
    }

    /**
     * 流式合成（当前为整段单元素流）。
     *
     * @param text 文本内容
     * @return 响应式数据流
     */
    public Flux<byte[]> speakStream(String text) {
        return ttsService.streamSynthesize(text, null);
    }
}
