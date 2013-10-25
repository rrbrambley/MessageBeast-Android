package com.alwaysallthetime.adnlibutils.db;

import java.util.HashSet;

public class HashtagInstances {
    private String mName;
    private HashSet<String> mMessageIds;

    public HashtagInstances(String name, String messageId) {
        mName = name;
        mMessageIds = new HashSet<String>();
        mMessageIds.add(messageId);
    }

    public void addInstance(String messageId) {
        mMessageIds.add(messageId);
    }

    public int getNumInstances() {
        return mMessageIds.size();
    }
}
