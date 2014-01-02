package com.alwaysallthetime.adnlibutils.model;

import com.alwaysallthetime.adnlib.data.Channel;

import java.util.List;

public class ChannelRefreshResult {

    private boolean mSuccess;

    private Channel mChannel;
    private List<MessagePlus> mResponseData;
    private boolean mAppended;

    private Exception mException;

    public ChannelRefreshResult(Channel channel, List<MessagePlus> responseData, boolean appended) {
        mChannel = channel;
        mResponseData = responseData;
        mAppended = appended;
        mSuccess = true;
    }

    public ChannelRefreshResult(Channel channel, Exception exception) {
        mChannel = channel;
        mException = exception;
        mSuccess = false;
    }

    public ChannelRefreshResult(Channel channel) {
        mChannel = channel;
        mSuccess = false;
    }

    public boolean isBlockedDueToUnsentMessages() {
        return !mSuccess && mException == null;
    }

    public boolean isSuccess() {
        return mSuccess;
    }

    public Channel getChannel() {
        return mChannel;
    }

    public List<MessagePlus> getMessages() {
        return mResponseData;
    }

    public boolean appended() {
        return mAppended;
    }

    public Exception getException() {
        return mException;
    }
}
