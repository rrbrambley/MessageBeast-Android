package com.alwaysallthetime.messagebeast.db;

import java.util.Date;

/**
 * An ActionMessageSpec is used to describe an Action Message.<br><br>
 *
 * @see com.alwaysallthetime.messagebeast.manager.ActionMessageManager
 */
public class ActionMessageSpec {
    private String mActionMessageId;
    private String mActionChannelId;
    private String mTargetMessageId;
    private String mTargetChannelId;
    private Date mTargetMessageDisplayDate;

    /**
     * Construct a new ActionMessageSpec.
     *
     * @param actionMessageId the id of the Action Message
     * @param actionChannelId the id of the Action Channel containing the Action Message
     * @param targetMessageId the id of the target Message for the Action Message
     * @param targetChannelId the id of the target Channel for the Action Message
     * @param targetMessageDisplayDate the display date of the target Message
     */
    public ActionMessageSpec(String actionMessageId, String actionChannelId, String targetMessageId, String targetChannelId, Date targetMessageDisplayDate) {
        mActionMessageId = actionMessageId;
        mActionChannelId = actionChannelId;
        mTargetMessageId = targetMessageId;
        mTargetChannelId = targetChannelId;
        mTargetMessageDisplayDate = targetMessageDisplayDate;
    }

    /**
     * Get the id of the Action Message.
     *
     * @return the id of the Action Message.
     */
    public String getActionMessageId() {
        return mActionMessageId;
    }

    /**
     * Get the id of the Action Channel containing the Action Message.
     *
     * @return the id of the Action Channel containing the Action Message.
     */
    public String getActionChannelId() {
        return mActionChannelId;
    }

    /**
     * Get the id of the target Message for the Action Message.
     *
     * @return the id of the target Message for the Action Message.
     */
    public String getTargetMessageId() {
        return mTargetMessageId;
    }

    /**
     * Get the id of the target Channel for the Action Message.
     *
     * @return the id of the target Channel for the Action Message.
     */
    public String getTargetChannelId() {
        return mTargetChannelId;
    }

    /**
     * Get the display date of the target Message.
     *
     * @return the display date of the target Message.
     */
    public Date getTargetMessageDisplayDate() {
        return mTargetMessageDisplayDate;
    }
}
