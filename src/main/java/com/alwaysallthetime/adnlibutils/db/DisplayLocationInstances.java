package com.alwaysallthetime.adnlibutils.db;

import com.alwaysallthetime.adnlibutils.model.DisplayLocation;

public class DisplayLocationInstances extends MessageMetadataInstances {

    private DisplayLocation mLocation;

    public DisplayLocationInstances(DisplayLocation location) {
        super();
        mLocation = location;
    }

    public DisplayLocationInstances(DisplayLocation location, String messageId) {
        this(location);
        addInstance(messageId);
    }

    public DisplayLocation getDisplayLocation() {
        return mLocation;
    }

    @Override
    public String getName() {
        return mLocation.getName();
    }
}
