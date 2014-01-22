package com.alwaysallthetime.adnlibutils.db;

/**
 * HashtagInstances is a MessageInstances in which all Messages have a Hashtag entity with
 * the same name.
 */
public class HashtagInstances extends MessageInstances {
    private String mName;

    /**
     * Construct a new HashtagInstances.
     *
     * @param name the name of the hashtag contained in all Messages in this MessageInstances
     */
    public HashtagInstances(String name) {
        super();
        mName = name;
    }

    /**
     * Construct a new HashtagInstances.
     *
     * @param name the name of the hashtag contained in all Messages in this MessageInstances
     * @param messageId a Message id to add to this HashtagInstances
     */
    public HashtagInstances(String name, String messageId) {
        this(name);
        addInstance(messageId);
    }

    /**
     * Get the name of the hashtag associated with all Messages in this HashtagInstances.
     *
     * @return the name of the hashtag associated with all Messages in this HashtagInstances.
     */
    @Override
    public String getName() {
        return mName;
    }
}
