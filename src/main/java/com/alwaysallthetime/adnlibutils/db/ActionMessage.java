package com.alwaysallthetime.adnlibutils.db;

public class ActionMessage {
    private String mActionMessageId;
    private String mActionChannelId;
    private String mTargetMessageId;
    private String mTargetChannelId;

    public ActionMessage(String actionMessageId, String actionChannelId, String targetMessageId, String targetChannelId) {
        mActionMessageId = actionMessageId;
        mActionChannelId = actionChannelId;
        mTargetMessageId = targetMessageId;
        mTargetChannelId = targetChannelId;
    }

    public String getActionMessageId() {
        return mActionMessageId;
    }

    public String getActionChannelId() {
        return mActionChannelId;
    }

    public String getTargetMessageId() {
        return mTargetMessageId;
    }

    public String getTargetChannelId() {
        return mTargetChannelId;
    }
}
