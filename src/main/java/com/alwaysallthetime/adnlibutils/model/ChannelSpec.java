package com.alwaysallthetime.adnlibutils.model;

import com.alwaysallthetime.adnlib.QueryParameters;
import com.alwaysallthetime.adnlibutils.filter.MessageFilter;

public class ChannelSpec {
    private MessageFilter mFilter;
    private String mType;
    private QueryParameters mQueryParameters;

    public ChannelSpec(String type, QueryParameters queryParameters) {
        mType = type;
        mQueryParameters = queryParameters;
    }

    public ChannelSpec(String type, QueryParameters queryParameters, MessageFilter filter) {
        this(type, queryParameters);
        mFilter = filter;
    }

    public String getType() {
        return mType;
    }

    public MessageFilter getMessageFilter() {
        return mFilter;
    }

    public QueryParameters getQueryParameters() {
        return mQueryParameters;
    }
}
