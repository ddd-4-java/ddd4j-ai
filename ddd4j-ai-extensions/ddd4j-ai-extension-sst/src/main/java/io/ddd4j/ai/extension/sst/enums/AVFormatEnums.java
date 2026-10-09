package io.ddd4j.ai.extension.sst.enums;

/**
 * 音视频格式枚举：key 供协议层传递格式标识。
 *
 * @author ddd4j
 */
public enum AVFormatEnums {

    /** 16kHz 16kbps 单声道 WAV 音频格式。 */
    Audio16Khz16KBitRateMonoWav("Audio16Khz16KBitRateMonoWav", ""),
    /** 16kHz 32kbps 单声道 MP3 音频格式。 */
    Audio16Khz16KBitRateMonoMp3("Audio16Khz32KBitRateMonoMp3", "");

    private String key;

    private String desc;

    /**
     * 枚举构造器。
     *
     * @param key  格式标识
     * @param desc 格式描述
     */
    AVFormatEnums(String key, String desc) {
        this.key = key;
        this.desc = desc;
    }

    /**
     * 获取格式标识。
     *
     * @return 格式标识
     */
    public String getKey() {
        return key;
    }

    /**
     * 获取格式描述。
     *
     * @return 格式描述
     */
    public String getDesc() {
        return desc;
    }
}
