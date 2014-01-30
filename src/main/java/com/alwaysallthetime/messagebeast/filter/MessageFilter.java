package com.alwaysallthetime.messagebeast.filter;

import com.alwaysallthetime.messagebeast.model.MessagePlus;

import java.util.TreeMap;

/**
 * A MessageFilter can be used to exclude Messages from a Map of Messages
 */
public abstract class MessageFilter {

    /**
     * Given an ordered Map of message id keys mapped to MessagePlus objects, get those that should be
     * excluded with this filter. This method should not actually remove any messages from the Map
     * that is passed to it.
     *
     * @param messages an ordered Map of message id keys mapped to MessagePlus objects
     * @return a TreeMap with message times in millis as keys, mapped to MessagePlus objects that should be excluded
     * with this filter.
     */
    public abstract TreeMap<Long, MessagePlus> getExcludedResults(TreeMap<Long, MessagePlus> messages);
}
