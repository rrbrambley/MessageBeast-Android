package com.alwaysallthetime.adnlibutils.model;

import com.alwaysallthetime.adnlibutils.db.OrderedMessageBatch;

import java.util.LinkedHashMap;

public abstract class MessageFilter {
    public abstract LinkedHashMap<String, MessagePlus> getExcludedResults(OrderedMessageBatch batch);
}
