package io.ddd4j.ai.extension.sst.enums;

/**
 * 音频转码目标枚举：key 供转码服务路由目标格式。
 *
 * @author ddd4j
 */
public enum ConvertKeyEnums {

    /** WAV 转 MP3。 */
    WAV2MP3("wav2mpa", ""),
    /** WAV 转 AMR。 */
    WAV2AMR("wav2arm", ""),
    /** WAV 转 PCM（用于语音转文本）。 */
    WAV2PCM("stt", "");

    private String key;

    private String desc;

    /**
     * 枚举构造器。
     *
     * @param key  转码目标标识
     * @param desc 转码目标描述
     */
    ConvertKeyEnums(String key, String desc) {
        this.key = key;
        this.desc = desc;
    }

    /**
     * 获取转码目标描述。
     *
     * @return 转码目标描述
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 获取转码目标标识。
     *
     * @return 转码目标标识
     */
    public String getKey() {
        return key;
    }
}
