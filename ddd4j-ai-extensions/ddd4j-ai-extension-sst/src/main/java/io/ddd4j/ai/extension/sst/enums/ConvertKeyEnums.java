package io.ddd4j.ai.extension.sst.enums;

/**
 * 音频转码键枚举：key 为通道协议约定的转换动作标识。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public enum ConvertKeyEnums {


    /** WAV → MP3 转码（key 按协议字面量保留）。 */
    WAV2MP3("wav2mpa",""),
    /** WAV → AMR 转码（key 按协议字面量保留）。 */
    WAV2AMR("wav2arm",""),
    /** WAV → PCM（语音识别用，key 为 stt）。 */
    WAV2PCM("stt","");

    private String key;

    private String desc;

    ConvertKeyEnums(String key,String desc) {
        this.key = key;
        this.desc = desc;
    }


    /**
     * 获取转换描述。
     *
     * @return 转换描述（可能为空串）
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 获取转换 key。
     *
     * @return 转换 key
     */
    public String getKey() {
        return key;
    }
}
