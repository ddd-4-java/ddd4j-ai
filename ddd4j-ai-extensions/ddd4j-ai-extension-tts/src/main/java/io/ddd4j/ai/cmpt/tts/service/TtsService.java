package io.ddd4j.ai.cmpt.tts.service;

import reactor.core.publisher.Flux;

/**
 * 文本转语音端口：合成完整音频字节或流式输出。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface TtsService {

    /**
     * 合成文本为完整音频字节（mp3 流）。
     *
     * @param text  待合成文本
     * @param voice 音色 shortName（如 zh-CN-XiaoxiaoNeural）；null 用默认音色
     * @return 完整音频字节
     * @throws Exception 网络不可用 / 合成失败
     */
    byte[] synthesize(String text, String voice) throws Exception;

    /**
     * 流式合成：按音频分片下发（当前实现为整段单元素流，便于后续分片演进）。
     *
     * @param text  待合成文本
     * @param voice 音色 shortName；null 用默认音色
     * @return 音频分片流
     */
    Flux<byte[]> streamSynthesize(String text, String voice);
}
