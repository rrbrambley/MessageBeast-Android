package com.alwaysallthetime.adnlibutils.filter;

import com.alwaysallthetime.adnlibutils.db.MessageEntityInstances;

import java.util.LinkedHashMap;

public abstract class MessageEntityInstancesFilter {
    public abstract void filterInstances(LinkedHashMap<String, ? extends MessageEntityInstances> instances);
}
