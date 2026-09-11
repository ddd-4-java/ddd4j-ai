package io.ddd4j.ai.extension.tts.service.impl;

import io.ddd4j.ai.extension.tts.service.TtsService;
import io.github.whitemagic2014.tts.TTS;
import io.github.whitemagic2014.tts.bean.Voice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;

/**
 * Edge TTS 实现：调用微软免费在线合成服务（无需密钥），输出 mp3 流。
 *
 * <p><b>关于流式语义</b>：whitemagic tts-edge-java 不暴露 mp3 帧级回调，因此
 * {@link #streamSynthesize(String, String)} 当前实现是先阻塞合成完整 mp3，再以
 * <b>单元素</b> {@code Flux<byte[]} 返回。若业务方需要真正帧级流式，应改用
 * {@link DashScopeRealtimeTtsService}（基于 DashScope WebSocket Realtime API，TTFA ~97ms）。
 *
 * <p>本类保留此实现是作为：
 * <ul>
 *   <li>无 DashScope API key 时的兜底后端</li>
 *   <li>{@link FallbackTtsRouter} 链路中的次选</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class EdgeTtsService implements TtsService {

    private static final Logger log = LoggerFactory.getLogger(EdgeTtsService.class);

    private final String defaultVoice;

    public EdgeTtsService(String defaultVoice) {
        this.defaultVoice = defaultVoice;
    }

    @Override
    public byte[] synthesize(String text, String voice) throws Exception {
        Voice target = new Voice();
        target.setShortName(voice == null || voice.isBlank() ? defaultVoice : voice);
        try (ByteArrayOutputStream out = new TTS(target).formatMp3().transToAudioStream()) {
            return out.toByteArray();
        }
    }

    @Override
    public Flux<byte[]> streamSynthesize(String text, String voice) {
        // Edge TTS 客户端限制：整段合成后才返回 mp3 字节，因此用单元素 Flux 暴露
        // 真正的帧级流式请用 DashScopeRealtimeTtsService
        log.debug("EdgeTtsService.streamSynthesize：单元素流（whitemagic SDK 不支持帧级流式）");
        return Flux.defer(() -> {
            try {
                return Flux.just(synthesize(text, voice));
            } catch (Exception e) {
                return Flux.error(e);
            }
        });
    }
}