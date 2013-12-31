package com.alwaysallthetime.adnlibutils.model;

import java.util.ArrayList;
import java.util.List;

public class ChannelSpecSet {

    private List<ChannelSpec> mChannelSpecs;

    public ChannelSpecSet(ChannelSpec... channels) {
        mChannelSpecs = new ArrayList<ChannelSpec>(channels.length);
        for(ChannelSpec spec : channels) {
            mChannelSpecs.add(spec);
        }
    }

    public List<ChannelSpec> getChannelSpecs() {
        return mChannelSpecs;
    }

    public int getNumChannels() {
        return mChannelSpecs.size();
    }
}
