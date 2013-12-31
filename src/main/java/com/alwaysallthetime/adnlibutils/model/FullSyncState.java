package com.alwaysallthetime.adnlibutils.model;

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
