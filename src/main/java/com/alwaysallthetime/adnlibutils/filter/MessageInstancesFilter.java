package com.alwaysallthetime.adnlibutils.filter;

import com.alwaysallthetime.adnlibutils.db.MessageInstances;

import java.util.LinkedHashMap;

public abstract class MessageInstancesFilter {
    public abstract void filterInstances(LinkedHashMap<String, ? extends MessageInstances> instances);
}
