package com.alwaysallthetime.adnlibutils.model;

import com.alwaysallthetime.adnlib.QueryParameters;

public class ChannelSpec {
    private String mType;
    private QueryParameters mQueryParameters;

    public ChannelSpec(String type, QueryParameters queryParameters) {
        mType = type;
        mQueryParameters = queryParameters;
    }

    public String getType() {
        return mType;
    }

    public QueryParameters getQueryParameters() {
        return mQueryParameters;
    }
}
