package io.ddd4j.ai.cmpt.sst.enums;

public enum AVFormatEnums {

    Audio16Khz16KBitRateMonoWav("Audio16Khz16KBitRateMonoWav",""),
    Audio16Khz16KBitRateMonoMp3("Audio16Khz32KBitRateMonoMp3",""),
    ;




    private String key;

    private String desc;
    AVFormatEnums(String key, String desc) {
        this.key = key;
        this.desc = desc;
    }

    public String getKey() {
        return key;
    }

    public String getDesc() {
        return desc;
    }
}
