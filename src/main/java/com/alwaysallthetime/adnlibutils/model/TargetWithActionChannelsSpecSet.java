package com.alwaysallthetime.adnlibutils.model;

import java.util.ArrayList;
import java.util.List;

public class TargetWithActionChannelsSpecSet {
    private ChannelSpec mTargetChannelSpec;
    private List<String> mActionChannelActionTypes;

    public TargetWithActionChannelsSpecSet(ChannelSpec targetChannelSpec, String... actionChannelActionTypes) {
        mTargetChannelSpec = targetChannelSpec;
        mActionChannelActionTypes = new ArrayList<String>(actionChannelActionTypes.length);
        for(String type : actionChannelActionTypes) {
            mActionChannelActionTypes.add(type);
        }
    }

    public ChannelSpec getTargetChannelSpec() {
        return mTargetChannelSpec;
    }

    public List<String> getActionChannelActionTypes() {
        return mActionChannelActionTypes;
    }

    public int getNumActionChannels() {
        return mActionChannelActionTypes.size();
    }

    public String getActionChannelActionTypeAtIndex(int index) {
        return mActionChannelActionTypes.get(index);
    }
}
