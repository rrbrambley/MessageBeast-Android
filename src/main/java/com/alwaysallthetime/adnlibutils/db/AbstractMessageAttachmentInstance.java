package com.alwaysallthetime.adnlibutils.db;

import java.util.LinkedHashSet;
import java.util.Set;

public abstract class AbstractMessageAttachmentInstance {
    protected LinkedHashSet<String> mMessageIds;

    public AbstractMessageAttachmentInstance() {
        mMessageIds = new LinkedHashSet<String>();
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

    public LinkedHashSet<String> getMessageIds() {
        return mMessageIds;
    }

    public abstract String getName();
}
