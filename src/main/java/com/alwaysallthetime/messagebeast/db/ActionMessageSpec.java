package com.alwaysallthetime.messagebeast.db;

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

    /**
     * Construct a new ActionMessageSpec.
     *
     * @param actionMessageId the id of the Action Message
     * @param actionChannelId the id of the Action Channel containing the Action Message
     * @param targetMessageId the id of the target Message for the Action Message
     * @param targetChannelId the id of the target Channel for the Action Message
     */
    public ActionMessageSpec(String actionMessageId, String actionChannelId, String targetMessageId, String targetChannelId) {
        mActionMessageId = actionMessageId;
        mActionChannelId = actionChannelId;
        mTargetMessageId = targetMessageId;
        mTargetChannelId = targetChannelId;
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
}
