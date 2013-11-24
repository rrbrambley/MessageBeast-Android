package com.alwaysallthetime.adnlibutils.db;

public class PendingMessageDeletion {

    private final String mMessageId;
    private final String mChannelId;
    private final boolean mDeleteAssociatedFiles;

    public PendingMessageDeletion(String messageId, String channelId, boolean deleteAssociatedFiles) {
        mMessageId = messageId;
        mChannelId = channelId;
        mDeleteAssociatedFiles = deleteAssociatedFiles;
    }

    public String getMessageId() {
        return mMessageId;
    }

    public String getChannelId() {
        return mChannelId;
    }

    public boolean isDeleteAssociatedFiles() {
        return mDeleteAssociatedFiles;
    }
}
