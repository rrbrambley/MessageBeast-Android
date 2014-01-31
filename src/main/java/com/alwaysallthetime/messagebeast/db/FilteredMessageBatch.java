package com.alwaysallthetime.messagebeast.db;

import com.alwaysallthetime.messagebeast.filter.MessageFilter;
import com.alwaysallthetime.messagebeast.model.MessagePlus;

import java.util.Iterator;
import java.util.TreeMap;

/**
 * A FilteredMessageBatch is an OrderedMessageBatch with a MessageFilter applied.
 * The getMessages() method thus returns the Messages Map with a subset of the entries
 * removed, while getExcludedMessages() returns a Map of all entries that were removed.
 */
public class FilteredMessageBatch extends OrderedMessageBatch {
    private TreeMap<Long, MessagePlus> mExcludedMessages;

    private FilteredMessageBatch(OrderedMessageBatch batch, TreeMap<Long, MessagePlus> filteredMessages) {
        super(batch.getMessages(), batch.getMinMaxPair());
        mExcludedMessages = filteredMessages;
    }

    /**
     * Get a new FilteredMessageBatch.
     *
     * @param batch the OrderedMessageBatch to filter
     * @param filter the MessageFilter to apply
     * @return a FilteredMessageBatch
     */
    public static FilteredMessageBatch getFilteredMessageBatch(OrderedMessageBatch batch, MessageFilter filter) {
        TreeMap<Long, MessagePlus> excludedResults = filter.getExcludedResults(batch.getMessages());

        TreeMap<Long, MessagePlus> results = batch.getMessages();
        Iterator<Long> iterator = excludedResults.keySet().iterator();
        while(iterator.hasNext()) {
            results.remove(iterator.next());
        }
        return new FilteredMessageBatch(batch, excludedResults);
    }

    /**
     * Get an ordered Map of Messages that were removed from the batch's messages Map.
     *
     * @return an ordered Map of Messages that were removed from the batch's messages Map
     */
    public TreeMap<Long, MessagePlus> getExcludedMessages() {
        return mExcludedMessages;
    }
}
