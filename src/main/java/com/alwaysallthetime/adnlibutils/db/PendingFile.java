package com.alwaysallthetime.adnlibutils.db;

import com.alwaysallthetime.adnlib.data.File;

public class PendingFile {
    private String mId;
    private byte[] mData;
    private String mType;
    private String mName;
    private String mMimeType;
    private String mKind;
    private boolean mIsPublic;

    public PendingFile(String id, byte[] data, String type, String name, String mimeType, String kind, boolean isPublic) {
        mId = id;
        mData = data;
        mType = type;
        mName = name;
        mMimeType = mimeType;
        mKind = kind;
        mIsPublic = isPublic;
    }

    public String getId() {
        return mId;
    }

    public byte[] getData() {
        return mData;
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
}
