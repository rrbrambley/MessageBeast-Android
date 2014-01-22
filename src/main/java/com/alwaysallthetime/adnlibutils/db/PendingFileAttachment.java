package com.alwaysallthetime.adnlibutils.db;

/**
 * A PendingFileAttachment corresponds to a PendingFile needed by a Message (e.g. for a file attachment or
 * OEmbed) that has yet to be uploaded.
 */
public class PendingFileAttachment {
    private String mPendingFileId;
    private boolean mIsOEmbed;

    /**
     * Construct a PendingFileAttachment.
     *
     * @param pendingFileId the id of the PendingFile associated with this PendingFileAttachment.
     * @param isOEmbed true if this attachment should be used to construct an OEmbed Annotation on
     *                 the associated Message, false if this attachment should be added to a
     *                 file list in an Attachments Annotation on the associated Message.
     */
    public PendingFileAttachment(String pendingFileId, boolean isOEmbed) {
        mPendingFileId = pendingFileId;
        mIsOEmbed = isOEmbed;
    }

    /**
     * Get the id of the PendingFile associated with this PendingFileAttachment.
     *
     * @return the id of the PendingFile associated with this PendingFileAttachment.
     */
    public String getPendingFileId() {
        return mPendingFileId;
    }

    /**
     * @return true if this attachment should be used to construct an OEmbed Annotation on
     * the associated Message, false if this attachment should be added to a
     * file list in an Attachments Annotation on the associated Message.
     */
    public boolean isOEmbed() {
        return mIsOEmbed;
    }
}
