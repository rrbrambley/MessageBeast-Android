package com.alwaysallthetime.adnlibutils.db;

public class AnnotationInstances extends MessageInstances {
    private String mType;

    public AnnotationInstances(String type) {
        super();
        mType = type;
    }

    public String getType() {
        return mType;
    }

    @Override
    public String getName() {
        return mType;
    }
}
