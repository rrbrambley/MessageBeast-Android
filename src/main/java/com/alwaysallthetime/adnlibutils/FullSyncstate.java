package com.alwaysallthetime.adnlibutils;

public enum FullSyncstate {
    NOT_STARTED,
    STARTED,
    COMPLETE;

    public static FullSyncstate fromOrdinal(int ordinal) {
        if(ordinal == 0) {
            return NOT_STARTED;
        } else if(ordinal == 1) {
            return STARTED;
        }
        return COMPLETE;
    }
}
