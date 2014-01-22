package com.alwaysallthetime.adnlibutils.filter;

import com.alwaysallthetime.adnlibutils.db.MessageInstances;

import java.util.LinkedHashMap;

/**
 * A MessageInstancesFilter can be used to remove MessageInstances from an ordered
 * Map of MessageInstances
 */
public abstract class MessageInstancesFilter {

    /**
     * Remove a subset of the provided MessageInstances.
     *
     * @param instances an ordered Map of MessageInstances
     */
    public abstract void filterInstances(LinkedHashMap<String, ? extends MessageInstances> instances);
}
