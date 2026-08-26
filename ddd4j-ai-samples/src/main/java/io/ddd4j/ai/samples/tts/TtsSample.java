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
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Component
public class TtsSample {

    private final TtsService ttsService;

    public TtsSample(TtsService ttsService) {
        this.ttsService = ttsService;
    }

    /** 合成完整 mp3 音频字节。 */
    public byte[] speak(String text) throws Exception {
        return ttsService.synthesize(text, null);
    }

    /** 流式合成（当前为整段单元素流）。 */
    public Flux<byte[]> speakStream(String text) {
        return ttsService.streamSynthesize(text, null);
    }
}
