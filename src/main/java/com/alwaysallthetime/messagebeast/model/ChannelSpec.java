package com.alwaysallthetime.messagebeast.model;

import com.alwaysallthetime.adnlib.QueryParameters;
import com.alwaysallthetime.messagebeast.filter.MessageFilter;

/**
 * A ChannelSpec is used to describe a Channel and properties associated with it.
 */
public class ChannelSpec {
    private MessageFilter mFilter;
    private String mType;
    private QueryParameters mQueryParameters;

    /**
     * Construct a new ChannelSpec.
     *
     * @param type the Channel type
     * @param queryParameters the QueryParameters to be used with the MessageManager when making
     *                        requests for this Channel
     */
    public ChannelSpec(String type, QueryParameters queryParameters) {
        mType = type;
        mQueryParameters = queryParameters;
    }

    /**
     * Construct a new ChannelSpec with a MesssageFilter.
     *
     * @param type the Channel type
     * @param queryParameters the QueryParameters to be used with the MessageManager when making
     *                        requests for this Channel
     * @param filter a MessageFilter to apply to results when retrieving Messages in this Channel
     */
    public ChannelSpec(String type, QueryParameters queryParameters, MessageFilter filter) {
        this(type, queryParameters);
        mFilter = filter;
    }

    /**
     * Get the Channel type.
     *
     * @return the Channel type
     */
    public String getType() {
        return mType;
    }

    /**
     * Get the MesageFilter associated with this ChannelSpec, or null if none exists.
     *
     * @return the MesageFilter associated with this ChannelSpec, or null if none exists.
     */
    public MessageFilter getMessageFilter() {
        return mFilter;
    }

    /**
     * Get the QueryParameters to be used with the MessageManager when making requests for this Channel.
     *
     * @return the QueryParameters to be used with the MessageManager when making requests for this Channel.
     */
    public QueryParameters getQueryParameters() {
        return mQueryParameters;
    }
}
