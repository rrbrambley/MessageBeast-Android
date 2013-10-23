package com.alwaysallthetime.adnlibutils;

import com.alwaysallthetime.adnlib.data.Message;
import com.alwaysallthetime.adnlibutils.model.DisplayLocation;

import java.util.Date;

public class MessagePlus {

    private Message mMessage;
    private Date mDisplayDate;
    private DisplayLocation mDisplayLocation;
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

    public DisplayLocation getDisplayLocation() {
        return mDisplayLocation;
    }

    public void setDisplayLocation(DisplayLocation location) {
        mDisplayLocation = location;
        mHasSetDisplayLocation = true;
    }

    public Message getMessage() {
        return mMessage;
    }
}
