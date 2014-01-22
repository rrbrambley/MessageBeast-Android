package com.alwaysallthetime.adnlibutils.model;

/**
 * FullSyncState describes the full sync state of a particular Channel. This is used in conjunction
 * with the MessageManager.
 *
 * @see com.alwaysallthetime.adnlibutils.manager.MessageManager#retrieveAndPersistAllMessages(String, com.alwaysallthetime.adnlibutils.manager.MessageManager.MessageManagerSyncResponseHandler)
 */
public enum FullSyncState {
    NOT_STARTED,
    STARTED,
    COMPLETE;

    public static FullSyncState fromOrdinal(int ordinal) {
        if(ordinal == 0) {
            return NOT_STARTED;
        } else if(ordinal == 1) {
            return STARTED;
        }
        return COMPLETE;
    }
}
