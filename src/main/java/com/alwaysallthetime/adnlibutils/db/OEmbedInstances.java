package com.alwaysallthetime.adnlibutils.db;

public class OEmbedInstances extends AbstractMessageAttachmentInstance {
    private String mType;

    public OEmbedInstances(String type) {
        super();
        mType = type;
    }

    public String getType() {
        return mType;
    }
}
