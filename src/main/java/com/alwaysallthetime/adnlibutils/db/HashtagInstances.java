package com.alwaysallthetime.adnlibutils.db;

public class HashtagInstances extends AbstractMessageAttachmentInstance {
    private String mName;

    public HashtagInstances(String name) {
        super();
        mName = name;
    }

    public HashtagInstances(String name, String messageId) {
        this(name);
        addInstance(messageId);
    }

    @Override
    public String getName() {
        return mName;
    }
}
