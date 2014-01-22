package com.alwaysallthetime.messagebeast.model;

import java.util.ArrayList;
import java.util.List;

/**
 * TargetWithActionChannelSpecSet is like a ChannelSpecSet in which a target Channel depends on
 * one or more Action Channels.
 */
public class TargetWithActionChannelsSpecSet {
    private ChannelSpec mTargetChannelSpec;
    private List<String> mActionChannelActionTypes;

    /**
     * Construct a TargetWithActionChannelsSpecSet.
     *
     * @param targetChannelSpec the "main," or target Channel for the Action Channels
     * @param actionChannelActionTypes the action types for the Action Channels that are used with the
     *                                 target Channel
     */
    public TargetWithActionChannelsSpecSet(ChannelSpec targetChannelSpec, String... actionChannelActionTypes) {
        mTargetChannelSpec = targetChannelSpec;
        mActionChannelActionTypes = new ArrayList<String>(actionChannelActionTypes.length);
        for(String type : actionChannelActionTypes) {
            mActionChannelActionTypes.add(type);
        }
    }

    /**
     * Get the ChannelSpec for the target Channel.
     *
     * @return the ChannelSpec for the target Channel.
     */
    public ChannelSpec getTargetChannelSpec() {
        return mTargetChannelSpec;
    }

    /**
     * Get the List of action types corresponding to the Action Channels used by the target
     * Channel in this TargetWithActionChannelsSpecSet
     *
     * @return the List of action types
     */
    public List<String> getActionChannelActionTypes() {
        return mActionChannelActionTypes;
    }

    /**
     * @return the number of Action Channels in this TargetWithActionChannelsSpecSet
     */
    public int getNumActionChannels() {
        return mActionChannelActionTypes.size();
    }

    /**
     * Get the action type at a specific index in this TargetWithActionChannelsSpecSet's List
     * of action types.
     *
     * @param index the index
     * @return the action type at a specific index in this TargetWithActionChannelsSpecSet's List
     * of action types.
     */
    public String getActionChannelActionTypeAtIndex(int index) {
        return mActionChannelActionTypes.get(index);
    }
}
