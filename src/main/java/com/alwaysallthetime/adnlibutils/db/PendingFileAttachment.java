package com.alwaysallthetime.adnlibutils.db;

/**
 * A file that has yet to be uploaded, that is needed by a Message.
 */
public class PendingFileAttachment {
    private String mPendingFileId;
    private boolean mIsOEmbed;
    private String mMessageId;
    private String mChannelId;

    public PendingFileAttachment(String pendingFileId, boolean isOEmbed, String messageId, String channelId) {
        mPendingFileId = pendingFileId;
        mIsOEmbed = isOEmbed;
        mMessageId = messageId;
        mChannelId = channelId;
    }

    public String getPendingFileId() {
        return mPendingFileId;
    }

    public boolean isOEmbed() {
        return mIsOEmbed;
    }

    public String getMessageId() {
        return mMessageId;
    }

    public String getChannelId() {
        return mChannelId;
    }
}
