package com.alwaysallthetime.messagebeast.model;

import com.alwaysallthetime.adnlib.data.Channel;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * A ChannelRefreshResult is used to describe the results when retrieving Messages with the ChannelSyncManager.
 */
public class ChannelRefreshResult {

    private boolean mSuccess;

    private Channel mChannel;
    private List<MessagePlus> mResponseData;
    private boolean mAppended;
    private LinkedHashMap<String, MessagePlus> mExcludedResults;

    private Exception mException;

    /**
     * Constrcut a successful ChannelRefreshResult.
     *
     * @param channel the Channel from which Messages were obtained
     * @param messages the Messages obtained
     * @param appended true if the Messages were appended to the end of the existing Messages in
     *                 the MessageManager, false if they were prepended.
     */
    public ChannelRefreshResult(Channel channel, List<MessagePlus> messages, boolean appended) {
        mChannel = channel;
        mResponseData = messages;
        mAppended = appended;
        mSuccess = true;
    }

    /**
     * Construct a successful ChannelRefreshResult with a Map of excluded results
     *
     * @param channel the Channel from which Messages were obtained
     * @param messages the Messages obtained
     * @param appended true if the Messages were appended to the end of the existing Messages in
     *                 the MessageManager, false if they were prepended.
     * @param excludedResults an ordered Map of results that were already removed from the provided Messsages
     */
    public ChannelRefreshResult(Channel channel, List<MessagePlus> messages, boolean appended, LinkedHashMap<String, MessagePlus> excludedResults) {
        this(channel, messages, appended);
        mExcludedResults = excludedResults;
    }

    /**
     * Construct a failed ChannelRefreshResult.
     * isSuccess() will return false on this result.
     *
     * @param channel the Channel
     * @param exception the Exception
     */
    public ChannelRefreshResult(Channel channel, Exception exception) {
        mChannel = channel;
        mException = exception;
        mSuccess = false;
    }

    /**
     * Construct a ChannelRefreshResult that is considered a failure because unsent Messages
     * exist in the Channel (thus blocking new Messages from being retrieved).
     *
     * @param channel the Channel
     */
    public ChannelRefreshResult(Channel channel) {
        mChannel = channel;
        mSuccess = false;
    }

    /**
     * @return true if the Channel associated with this result has unsent Messages that are blocking
     * more from being retrieved, false otherwise.
     */
    public boolean isBlockedDueToUnsentMessages() {
        return !mSuccess && mException == null;
    }

    /**
     * @return true if this result was successful
     */
    public boolean isSuccess() {
        return mSuccess;
    }

    /**
     * @return the Channel associated with this result
     */
    public Channel getChannel() {
        return mChannel;
    }

    /**
     * Get the List of MessagePlus Objects constructed after retrieving more Messages.
     * This will be null if isSuccess() returns false.
     *
     * @return the List of MessagePlus Objects constructed after retrieving more Messages, or
     * null when isSuccess() returns false.
     */
    public List<MessagePlus> getMessages() {
        return mResponseData;
    }

    /**
     * Get the ordered Map of excluded Messages that were removed prior to constructing this
     * ChannelRefreshResult. This can be null.
     *
     * @return the ordered Map of excluded Messages that were removed prior to constructing this
     * ChannelRefreshResult, or null if none exists.
     */
    public LinkedHashMap<String, MessagePlus> getExcludedResults() {
        return mExcludedResults;
    }

    /**
     * @return true if this was a successful result in which Messages were appended to the end of
     * the Message Map maintained by the MessageManager, false otherwise
     */
    public boolean appended() {
        return mAppended;
    }

    /**
     * Get the Exception associated with this ChannelRefreshResult. This can be null.
     *
     * @return the Exception associated with this ChannelRefreshResult, or null if none exists.
     */
    public Exception getException() {
        return mException;
    }
}
