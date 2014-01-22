package com.alwaysallthetime.messagebeast.db;

import com.alwaysallthetime.messagebeast.manager.MinMaxPair;
import com.alwaysallthetime.messagebeast.model.MessagePlus;

import java.util.LinkedHashMap;

/**
 * An OrderedMessageBatch consists of an ordered Map of MessagePlus objects whose keys are
 * Message ids, as well as a MinMaxPair representing the min and max ids of the Messages in the Map.
 */
public class OrderedMessageBatch {
    private LinkedHashMap<String, MessagePlus> mMessages;
    private MinMaxPair mMinMaxPair;

    /**
     * Construct an OrderedMessageBatch.
     *
     * @param messages a Map whose keys are Message ids mapped to values that are MessagePlus objects.
     * @param minMaxPair a MinMaxPair containing the min and max Message ids in the Set of Map keys.
     */
    public OrderedMessageBatch(LinkedHashMap<String, MessagePlus> messages, MinMaxPair minMaxPair) {
        mMessages = messages;
        mMinMaxPair = minMaxPair;
    }

    /**
     * Get the Message Map, with keys that are Message ids mapped to values that are MessagePlus objects.
     *
     * @return the Map of Messages
     */
    public LinkedHashMap<String, MessagePlus> getMessages() {
        return mMessages;
    }

    /**
     * Get the MinMaxPair containing the min and max Message ids associated with the Message in this batch
     *
     * @return the MinMaxPair containing the min and max Message ids associated with the Message in this batch
     */
    public MinMaxPair getMinMaxPair() {
        return mMinMaxPair;
    }
}
