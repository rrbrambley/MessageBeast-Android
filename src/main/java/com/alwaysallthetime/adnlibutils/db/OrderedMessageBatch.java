package com.alwaysallthetime.adnlibutils.db;

import com.alwaysallthetime.adnlib.data.Message;
import com.alwaysallthetime.adnlibutils.manager.MinMaxPair;

import java.util.LinkedHashMap;

public class OrderedMessageBatch {
    private LinkedHashMap<String, Message> mMessages;
    private MinMaxPair mMinMaxPair;

    public OrderedMessageBatch(LinkedHashMap<String, Message> messages, MinMaxPair minMaxPair) {
        mMessages = messages;
        mMinMaxPair = minMaxPair;
    }

    public LinkedHashMap<String, Message> getMessages() {
        return mMessages;
    }

    public MinMaxPair getMinMaxPair() {
        return mMinMaxPair;
    }
}
