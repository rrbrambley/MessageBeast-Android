package com.alwaysallthetime.adnlibutils.db;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * An abstract class used to group a set of Messages that all contain
 * the same type of Message metadata. Metadata
 */
public abstract class MessageInstances {
    protected LinkedHashSet<String> mMessageIds;

    public MessageInstances() {
        mMessageIds = new LinkedHashSet<String>();
    }

    public void addInstance(String messageId) {
        mMessageIds.add(messageId);
    }

    public void addAll(Set<String> messageIds) {
        mMessageIds.addAll(messageIds);
    }

    public boolean removeAll(Set<String> messageIds) {
        return mMessageIds.removeAll(messageIds);
    }

    public int getNumInstances() {
        return mMessageIds.size();
    }

    public LinkedHashSet<String> getMessageIds() {
        return mMessageIds;
    }

    public abstract String getName();
}
