package com.alwaysallthetime.messagebeast.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A ChannelSpecSet is a set of ChannelSpecs
 */
public class ChannelSpecSet {

    private List<ChannelSpec> mChannelSpecs;

    /**
     * Construct a new ChannelSpecSet
     *
     * @param channels the ChannelSpecs to add to this ChannelSpecSet
     */
    public ChannelSpecSet(ChannelSpec... channels) {
        mChannelSpecs = new ArrayList<ChannelSpec>(channels.length);
        for(ChannelSpec spec : channels) {
            mChannelSpecs.add(spec);
        }
    }

    /**
     * Get the ChannelSpecs in this ChannelSpecSet.
     *
     * @return a List of ChannelSpecs in this ChannelSpecSet.
     */
    public List<ChannelSpec> getChannelSpecs() {
        return mChannelSpecs;
    }

    /**
     * Get the number of ChannelSpecs in this ChannelSpecSet.
     *
     * @return the number of ChannelSpecs in this ChannelSpecSet.
     */
    public int getNumChannels() {
        return mChannelSpecs.size();
    }

    /**
     * Get a ChannelSpec at a specific index in the List of ChannelSpecs in this ChannelSpecSet.
     *
     * @param index the index
     * @return a ChannelSpec at the provided index in the List of ChannelSpecs in this ChannelSpecSet.
     */
    public ChannelSpec getChannelSpecAtIndex(int index) {
        return mChannelSpecs.get(index);
    }
}
