package io.ddd4j.ai.cmpt.tts.service.impl;

import io.ddd4j.ai.cmpt.tts.service.TtsService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link EdgeTtsService} 联机冒烟测试：需外网访问微软 Edge TTS 服务，
 * 默认禁用（CI 无外网），本地手动验证时开启。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Disabled("需要外网访问 Edge TTS 服务，本地手动验证")
class EdgeTtsServiceTest {

    @Test
    void synthesize_returnsMp3Bytes() throws Exception {
        TtsService service = new EdgeTtsService("zh-CN-XiaoxiaoNeural");
        byte[] audio = service.synthesize("你好，ddd4j-ai", null);
        assertThat(audio).isNotEmpty();
        // MP3 帧头同步字 0xFF 0xFB / 0xFF 0xF3
        assertThat(audio[0] & 0xFF).isEqualTo(0xFF);
    }
}
