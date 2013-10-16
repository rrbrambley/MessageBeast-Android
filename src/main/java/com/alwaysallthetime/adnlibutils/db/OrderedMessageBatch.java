package com.alwaysallthetime.adnlibutils.db;

import com.alwaysallthetime.adnlibutils.MessagePlus;
import com.alwaysallthetime.adnlibutils.manager.MinMaxPair;

import java.util.LinkedHashMap;

public class OrderedMessageBatch {
    private LinkedHashMap<String, MessagePlus> mMessages;
    private MinMaxPair mMinMaxPair;

    public OrderedMessageBatch(LinkedHashMap<String, MessagePlus> messages, MinMaxPair minMaxPair) {
        mMessages = messages;
        mMinMaxPair = minMaxPair;
    }

    public LinkedHashMap<String, MessagePlus> getMessages() {
        return mMessages;
    }

    public MinMaxPair getMinMaxPair() {
        return mMinMaxPair;
    }
}
