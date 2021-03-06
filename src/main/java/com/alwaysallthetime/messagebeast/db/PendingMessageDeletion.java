package com.alwaysallthetime.messagebeast.db;

/**
 * A PendingMessageDeletion represents a Message that is intended to be deleted at a later time.
 */
public class PendingMessageDeletion {

    private final String mMessageId;
    private final String mChannelId;

    /**
     * Construct a PendingMessageDeletion.
     *
     * @param messageId the id of the Message to be deleted.
     * @param channelId the id of the Channel in which the Message is contained.
     */
    public PendingMessageDeletion(String messageId, String channelId) {
        mMessageId = messageId;
        mChannelId = channelId;
    }

    /**
     * Get the id of the Message to be deleted.
     *
     * @return the id of the Message to be deleted
     */
    public String getMessageId() {
        return mMessageId;
    }

    /**
     * Get the id of the Channel in which the Message is contained.
     *
     * @return the id of the Channel in which the Message is contained.
     */
    public String getChannelId() {
        return mChannelId;
    }
}
