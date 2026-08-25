package io.ddd4j.ai.cmpt.tts.service.impl;

import io.ddd4j.ai.cmpt.tts.service.TtsService;
import io.github.whitemagic2014.tts.TTS;
import io.github.whitemagic2014.tts.bean.Voice;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;

/**
 * Edge TTS 实现：调用微软免费在线合成服务（无需密钥），输出 mp3 流。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class EdgeTtsService implements TtsService {

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
        return Flux.defer(() -> {
            try {
                return Flux.just(synthesize(text, voice));
            } catch (Exception e) {
                return Flux.error(e);
            }
        });
    }
}
