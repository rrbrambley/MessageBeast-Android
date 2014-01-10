package com.alwaysallthetime.adnlibutils.filter;

import com.alwaysallthetime.adnlibutils.model.MessagePlus;

import java.util.LinkedHashMap;

public abstract class MessageFilter {
    public abstract LinkedHashMap<String, MessagePlus> getExcludedResults(LinkedHashMap<String, MessagePlus> messages);
}
