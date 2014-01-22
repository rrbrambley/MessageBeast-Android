package com.alwaysallthetime.messagebeast.model;

import com.alwaysallthetime.adnlib.data.Channel;

import java.util.HashMap;
import java.util.Map;

/**
 * A ChannelRefreshResultSet contains ChannelRefreshResults for multiple Channels.
 */
public class ChannelRefreshResultSet {

    private Map<String, ChannelRefreshResult> mResults;

    /**
     * Construct an empty ChannelRefreshResult.
     */
    public ChannelRefreshResultSet() {
        mResults = new HashMap<String, ChannelRefreshResult>();
    }

    /**
     * Add a ChannelRefreshResult. Only one result per Channel is allowed.
     *
     * @param refreshResult the ChannelRefreshResult to add
     */
    public void addRefreshResult(ChannelRefreshResult refreshResult) {
        Channel channel = refreshResult.getChannel();
        mResults.put(channel.getId(), refreshResult);
    }

    /**
     * Get a ChannelRefreshResult for the Channel with the provided id.
     *
     * @param channelId the Channel id
     * @return a ChannelRefreshResult for the Channel with the provided id, or null
     * if no result for the Channel exists in this ChannelRefreshResultSet
     */
    public ChannelRefreshResult getChannelRefreshResult(String channelId) {
        return mResults.get(channelId);
    }
}
