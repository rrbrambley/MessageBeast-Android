package com.alwaysallthetime.adnlibutils.db;

import android.net.Uri;

import com.alwaysallthetime.adnlib.data.File;

public class PendingFile {
    private String mId;
    private Uri mUri;
    private String mType;
    private String mName;
    private String mMimeType;
    private String mKind;
    private boolean mIsPublic;
    private int mNumSendAttempts;

    public PendingFile(String id, Uri uri, String type, String name, String mimeType, String kind, boolean isPublic, int sendAttempts) {
        mId = id;
        mUri = uri;
        mType = type;
        mName = name;
        mMimeType = mimeType;
        mKind = kind;
        mIsPublic = isPublic;
        mNumSendAttempts = sendAttempts;
    }

    public String getId() {
        return mId;
    }

    public Uri getUri() {
        return mUri;
    }

    public String getType() {
        return mType;
    }

    public String getName() {
        return mName;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public String getKind() {
        return mKind;
    }

    public boolean isPublic() {
        return mIsPublic;
    }

    public File getFile() {
        return new File(mKind, mType, mName, mIsPublic);
    }

    public int getNumSendAttempts() {
        return mNumSendAttempts;
    }

    public int incrementSendAttempts() {
        mNumSendAttempts++;
        return mNumSendAttempts;
    }
}
