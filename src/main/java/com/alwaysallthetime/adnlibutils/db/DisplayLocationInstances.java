package com.alwaysallthetime.adnlibutils.db;

import com.alwaysallthetime.adnlibutils.model.DisplayLocation;

/**
 * DisplayLocationInstances is a MessageInstances in which all Messages have
 * the same associated DisplayLocation.
 */
public class DisplayLocationInstances extends MessageInstances {

    private DisplayLocation mLocation;

    /**
     * Construct a DisplayLocationInstances.
     *
     * @param location the DisplayLocation associated with all Messages in this MessageInstances
     */
    public DisplayLocationInstances(DisplayLocation location) {
        super();
        mLocation = location;
    }

    /**
     * Get the DisplayLocation associated with all Messages in this MessageInstances.
     *
     * @return the DisplayLocation associated with all Messages in this MessageInstances.
     */
    public DisplayLocation getDisplayLocation() {
        return mLocation;
    }

    /**
     * Get the name of the DisplayLocation associated with all Messages in this MessageInstances.
     *
     * @return the name of the DisplayLocation associated with all Messages in this MessageInstances
     */
    @Override
    public String getName() {
        return mLocation.getName();
    }
}
