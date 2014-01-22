package com.alwaysallthetime.adnlibutils.db;

import com.alwaysallthetime.adnlibutils.filter.MessageFilter;
import com.alwaysallthetime.adnlibutils.model.MessagePlus;

import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * A FilteredMessageBatch is an OrderedMessageBatch with a MessageFilter applied.
 * The getMessages() method thus returns the Messages Map with a subset of the entries
 * removed, while getExcludedMessages() returns a Map of all entries that were removed.
 */
public class FilteredMessageBatch extends OrderedMessageBatch {
    private LinkedHashMap<String, MessagePlus> mExcludedMessages;

    private FilteredMessageBatch(OrderedMessageBatch batch, LinkedHashMap<String, MessagePlus> filteredMessages) {
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
        LinkedHashMap<String, MessagePlus> excludedResults = filter.getExcludedResults(batch.getMessages());

        LinkedHashMap<String, MessagePlus> results = batch.getMessages();
        Iterator<String> iterator = excludedResults.keySet().iterator();
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
    public LinkedHashMap<String, MessagePlus> getExcludedMessages() {
        return mExcludedMessages;
    }
}
