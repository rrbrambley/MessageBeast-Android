package com.alwaysallthetime.adnlibutils.db;

import com.alwaysallthetime.adnlibutils.model.MessageFilter;
import com.alwaysallthetime.adnlibutils.model.MessagePlus;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class FilteredMessageBatch extends OrderedMessageBatch {
    private LinkedHashMap<String, MessagePlus> mExcludedMessages;

    private FilteredMessageBatch(OrderedMessageBatch batch, LinkedHashMap<String, MessagePlus> filteredMessages) {
        super(batch.getMessages(), batch.getMinMaxPair());
        mExcludedMessages = filteredMessages;
    }

    public static FilteredMessageBatch getFilteredMessageBatch(OrderedMessageBatch batch, MessageFilter filter) {
        LinkedHashMap<String, MessagePlus> excludedResults = filter.getExcludedResults(batch.getMessages());

        LinkedHashMap<String, MessagePlus> results = batch.getMessages();
        Iterator<String> iterator = excludedResults.keySet().iterator();
        while(iterator.hasNext()) {
            results.remove(iterator.next());
        }
        return new FilteredMessageBatch(batch, excludedResults);
    }

    public LinkedHashMap<String, MessagePlus> getExcludedMessages() {
        return mExcludedMessages;
    }
}
