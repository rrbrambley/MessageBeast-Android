package com.alwaysallthetime.adnlibutils.db;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractMessageAttachmentInstance {
    protected HashSet<String> mMessageIds;

    public AbstractMessageAttachmentInstance() {
        mMessageIds = new HashSet<String>();
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
