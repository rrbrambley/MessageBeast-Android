package com.alwaysallthetime.adnlibutils.db;

/**
 * A file that has yet to be uploaded, that is needed by a Message.
 */
public class PendingFileAttachment {
    private String mPendingFileId;
    private boolean mIsOEmbed;

    public PendingFileAttachment(String pendingFileId, boolean isOEmbed) {
        mPendingFileId = pendingFileId;
        mIsOEmbed = isOEmbed;
    }

    public String getPendingFileId() {
        return mPendingFileId;
    }

    public boolean isOEmbed() {
        return mIsOEmbed;
    }
}
