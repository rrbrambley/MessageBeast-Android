package com.alwaysallthetime.adnlibutils.model;

import java.util.LinkedHashMap;

public abstract class MessageFilter {
    public abstract LinkedHashMap<String, MessagePlus> getExcludedResults(LinkedHashMap<String, MessagePlus> messages);
}
