package com.alwaysallthetime.adnlibutils.db;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * An abstract class used to group a set of Messages that all have one or more features in common.
 */
public abstract class MessageInstances {
    protected LinkedHashSet<String> mMessageIds;

    public MessageInstances() {
        mMessageIds = new LinkedHashSet<String>();
    }

    /**
     * Add an instance.
     *
     * @param messageId the id of the Message instance to add
     */
    public void addInstance(String messageId) {
        mMessageIds.add(messageId);
    }

    /**
     * Add a Set of instances.
     *
     * @param messageIds the ids of the Message instances to add
     */
    public void addAll(Set<String> messageIds) {
        mMessageIds.addAll(messageIds);
    }

    /**
     * Remove a Set of instances.
     *
     * @param messageIds the ids of the Message instances to remove
     * @return true if 1 or more message ids was removed, false otherwise
     */
    public boolean removeAll(Set<String> messageIds) {
        return mMessageIds.removeAll(messageIds);
    }

    /**
     * Get the number of message ids associated with this MessageInstances.
     *
     * @return the number of message ids associated with this MessageInstances.
     */
    public int getNumInstances() {
        return mMessageIds.size();
    }

    /**
     * Get the Message ids.
     *
     * @return the Message ids in this MessageInstances
     */
    public LinkedHashSet<String> getMessageIds() {
        return mMessageIds;
    }

    /**
     * Get the name of this MessageInstances. The name is generally used to describe
     * the feature(s) in common had by the Messages associated with this MessageInstances.
     *
     * @return the name of this MessageInstances
     */
    public abstract String getName();
}
