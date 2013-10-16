package com.alwaysallthetime.adnlibutils;

import com.alwaysallthetime.adnlib.data.Message;

import java.util.Date;

public class MessagePlus {

    private Message mMessage;
    private Date mDisplayDate;
    private String mDisplayLocation;
    private boolean mHasSetDisplayLocation;

    public MessagePlus(Message message) {
        mMessage = message;
    }

    public Date getDisplayDate() {
        return mDisplayDate != null ? mDisplayDate : mMessage.getCreatedAt();
    }

    public void setDisplayDate(Date displayDate) {
        mDisplayDate = displayDate;
    }

    public boolean hasSetDisplayLocation() {
        return mHasSetDisplayLocation;
    }

    public String getDisplayLocation() {
        return mDisplayLocation;
    }

    public void setDisplayLocation(String location) {
        mDisplayLocation = location;
        mHasSetDisplayLocation = true;
    }

    public Message getMessage() {
        return mMessage;
    }
}
