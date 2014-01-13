package com.alwaysallthetime.adnlibutils.filter;

import com.alwaysallthetime.adnlibutils.db.MessageMetadataInstances;

import java.util.LinkedHashMap;

public abstract class MessageMetadataInstancesFilter {
    public abstract void filterInstances(LinkedHashMap<String, ? extends MessageMetadataInstances> instances);
}
