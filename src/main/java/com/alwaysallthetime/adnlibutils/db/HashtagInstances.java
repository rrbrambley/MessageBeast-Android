package com.alwaysallthetime.adnlibutils.db;

import java.util.HashSet;
import java.util.Set;

public class HashtagInstances {
    private String mName;
    private HashSet<String> mMessageIds;

    public HashtagInstances(String name) {
        mName = name;
        mMessageIds = new HashSet<String>();
    }

    public HashtagInstances(String name, String messageId) {
        this(name);
        mMessageIds.add(messageId);
    }

    public void addInstance(String messageId) {
        mMessageIds.add(messageId);
    }

    public void addAll(Set<String> messageIds) {
        mMessageIds.addAll(messageIds);
    }

    public int getNumInstances() {
        return mMessageIds.size();
    }

    public Set<String> getMessageIds() {
        return mMessageIds;
    }
}
