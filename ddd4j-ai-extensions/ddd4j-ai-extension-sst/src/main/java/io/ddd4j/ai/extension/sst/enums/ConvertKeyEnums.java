package io.ddd4j.ai.extension.sst.enums;

public enum ConvertKeyEnums {


    WAV2MP3("wav2mpa",""),
    WAV2AMR("wav2arm",""),
    WAV2PCM("stt","");

    private String key;

    private String desc;

    ConvertKeyEnums(String key,String desc) {
        this.key = key;
        this.desc = desc;
    }


    public String getDesc() {
        return desc;
    }

    public String getKey() {
        return key;
    }
}
