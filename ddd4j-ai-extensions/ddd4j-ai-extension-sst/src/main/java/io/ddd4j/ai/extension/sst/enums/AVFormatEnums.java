package io.ddd4j.ai.extension.sst.enums;

/**
 * 音视频格式枚举：key 为通道协议约定的格式标识（16kHz 16kbps 单声道 WAV 等）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public enum AVFormatEnums {

    /** 16kHz / 16kbps / 单声道 WAV。 */
    Audio16Khz16KBitRateMonoWav("Audio16Khz16KBitRateMonoWav",""),
    /** 16kHz / 32kbps / 单声道 MP3（key 按协议字面量保留）。 */
    Audio16Khz16KBitRateMonoMp3("Audio16Khz32KBitRateMonoMp3",""),
    ;




    private String key;

    private String desc;
    AVFormatEnums(String key, String desc) {
        this.key = key;
        this.desc = desc;
    }

    /**
     * 获取格式 key。
     *
     * @return 格式 key
     */
    public String getKey() {
        return key;
    }

    /**
     * 获取格式描述。
     *
     * @return 格式描述（可能为空串）
     */
    public String getDesc() {
        return desc;
    }
}
