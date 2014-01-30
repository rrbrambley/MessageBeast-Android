package com.alwaysallthetime.messagebeast.db;

import com.alwaysallthetime.messagebeast.manager.MinMaxDatePair;
import com.alwaysallthetime.messagebeast.model.MessagePlus;

import java.util.TreeMap;

/**
 * An OrderedMessageBatch consists of an ordered Map of MessagePlus objects whose keys are
 * Message times in millis, as well as a MinMaxDatePair representing the min and max times
 * of the Messages in the Map.
 */
public class OrderedMessageBatch {
    private TreeMap<Long, MessagePlus> mMessages;
    private MinMaxDatePair mMinMaxDatePair;

    /**
     * Construct an OrderedMessageBatch.
     *
     * @param messages a Map whose keys are Message ids mapped to values that are MessagePlus objects.
     * @param minMaxPair a MinMaxPair containing the min and max Message ids in the Set of Map keys.
     */
    public OrderedMessageBatch(TreeMap<Long, MessagePlus> messages, MinMaxDatePair minMaxPair) {
        mMessages = messages;
        mMinMaxDatePair = minMaxPair;
    }

    /**
     * Get the Message Map, with keys that are Message ids mapped to values that are MessagePlus objects.
     *
     * @return the Map of Messages
     */
    public TreeMap<Long, MessagePlus> getMessages() {
        return mMessages;
    }

    /**
     * Get the MinMaxDatePair containing the min and max Message times associated with the Message in this batch
     *
     * @return the MinMaxDatePair containing the min and max Message times associated with the Message in this batch
     */
    public MinMaxDatePair getMinMaxDatePair() {
        return mMinMaxDatePair;
    }
}
