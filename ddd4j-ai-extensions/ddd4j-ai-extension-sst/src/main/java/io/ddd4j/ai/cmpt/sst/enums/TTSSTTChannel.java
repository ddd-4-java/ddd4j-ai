package io.ddd4j.ai.cmpt.sst.enums;

public enum TTSSTTChannel {


    Azure("azure","微软azure"),

    ;

    private String channel;

    private String channelName;

    TTSSTTChannel(String channel,String channelName) {
        this.channel = channel;
        this.channelName = channelName;

    }

    public String getChannel() {
        return channel;
    }


    public String getChannelName() {
        return channelName;
    }
}
