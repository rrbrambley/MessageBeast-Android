package com.alwaysallthetime.adnlibutils.model;

import com.alwaysallthetime.adnlib.data.Channel;

import java.util.HashMap;
import java.util.Map;

public class ChannelRefreshResultSet {

    private Map<String, ChannelRefreshResult> mResults;

    public ChannelRefreshResultSet() {
        mResults = new HashMap<String, ChannelRefreshResult>();
    }

    public void addRefreshResult(ChannelRefreshResult refreshResult) {
        Channel channel = refreshResult.getChannel();
        mResults.put(channel.getId(), refreshResult);
    }

    public ChannelRefreshResult getChannelRefreshResult(String channelId) {
        return mResults.get(channelId);
    }
}
