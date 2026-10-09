package io.ddd4j.ai.extension.sst.enums;

/**
 * 语音服务渠道枚举：区分不同云厂商的语音通道。
 *
 * @author ddd4j
 */
public enum TTSSTTChannel {

    /** 微软 Azure 语音通道。 */
    Azure("azure", "微软azure");

    private String channel;

    private String channelName;

    /**
     * 枚举构造器。
     *
     * @param channel     渠道标识
     * @param channelName 渠道显示名
     */
    TTSSTTChannel(String channel, String channelName) {
        this.channel = channel;
        this.channelName = channelName;
    }

    /**
     * 获取渠道标识。
     *
     * @return 渠道标识
     */
    public String getChannel() {
        return channel;
    }

    /**
     * 获取渠道显示名。
     *
     * @return 渠道显示名
     */
    public String getChannelName() {
        return channelName;
    }
}
