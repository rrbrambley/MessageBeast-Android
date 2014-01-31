package com.alwaysallthetime.messagebeast.db;

import com.alwaysallthetime.messagebeast.manager.MinMaxPair;
import com.alwaysallthetime.messagebeast.model.MessagePlus;

import java.util.TreeMap;

/**
 * An OrderedMessageBatch consists of an ordered Map of MessagePlus objects whose keys are
 * Message times in millis. It also containss a MinMaxPair containing the min and max times
 * of the Messages in the Map, as well as the min and max ids of the Messages
 * in the Map.
 */
public class OrderedMessageBatch {
    private TreeMap<Long, MessagePlus> mMessages;
    private MinMaxPair mMinMaxPair;

    /**
     * Construct an OrderedMessageBatch.
     *
     * @param messages a Map whose keys are Message times in millis, mapped to values that are MessagePlus objects
     * @param minMaxPair a MinMaxPair containing the min and max Message ids and times associated with the Messages in this batch
     */
    public OrderedMessageBatch(TreeMap<Long, MessagePlus> messages, MinMaxPair minMaxPair) {
        mMessages = messages;
        mMinMaxPair = minMaxPair;
    }

    /**
     * Get the Message Map, with keys that are Message times in millis, mapped to values that are MessagePlus objects.
     *
     * @return the Map of Messages
     */
    public TreeMap<Long, MessagePlus> getMessages() {
        return mMessages;
    }

    /**
     * Get the MinMaxPair containing the min and max Message ids and times associated with the Messages in this batch
     *
     * @return the MinMaxPair containing the min and max Message ids and times associated with the Messages in this batch
     */
    public MinMaxPair getMinMaxPair() {
        return mMinMaxPair;
    }
}
