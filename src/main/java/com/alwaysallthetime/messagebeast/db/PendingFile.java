package com.alwaysallthetime.messagebeast.db;

import android.net.Uri;

import com.alwaysallthetime.adnlib.data.File;

/**
 * A PendingFile represents a local File that is pending upload to App.net.
 *
 * @see <a href="http://developers.app.net/docs/resources/file/">http://developers.app.net/docs/resources/file/</a>
 */
public class PendingFile {
    private String mId;
    private Uri mUri;
    private String mType;
    private String mName;
    private String mMimeType;
    private String mKind;
    private boolean mIsPublic;
    private int mNumSendAttempts;

    /**
     * Construct a new PendingFile.
     *
     * @param id the id of the PendingFile, typically a UUID. This is unrelated to the id of the
     *           File that will be assigned to the File by the App.net servers upon successful
     *           upload.
     * @param uri the Uri corresponding to the file's location on disk
     * @param type the type to be set on the App.net File's "type" field.
     * @param name the name of the App.net File
     * @param mimeType the MIME type of the file
     * @param kind the kind of the App.net file
     * @param isPublic true if the File is public, false otherwise
     * @param sendAttempts the number of times we have attempted to send the PendingFile with this id
     */
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

    /**
     * Get the id of the PendingFile. This is unrelated to the id of the
     * File that will be assigned to the File by the App.net servers upon successful
     * upload.
     *
     * @return the id of the PendingFile, typically a UUID
     */
    public String getId() {
        return mId;
    }

    /**
     * Get the Uri corresponding to the file's location on disk.
     *
     * @return the Uri corresponding to the file's location on disk.
     */
    public Uri getUri() {
        return mUri;
    }

    /**
     * Get the type to be set on the App.net File's "type" field.
     *
     * @return the type to be set on the App.net File's "type" field.
     */
    public String getType() {
        return mType;
    }

    /**
     * Get the name to be assigned to the App.net File object
     *
     * @return the name to be assigned to the App.net File object
     */
    public String getName() {
        return mName;
    }

    /**
     * Get the MIME type of the file.
     *
     * @return the MIME type of the file.
     */
    public String getMimeType() {
        return mMimeType;
    }

    /**
     * Get the File kind
     *
     * @return the File kind
     */
    public String getKind() {
        return mKind;
    }

    /**
     * @return true if the File should be public, false otherwise.
     */
    public boolean isPublic() {
        return mIsPublic;
    }

    /**
     * Get a File object to use when sending the POST request to App.net
     *
     * @return a File object to use when sending the POST request to App.net
     */
    public File getFile() {
        return new File(mKind, mType, mName, mIsPublic);
    }

    /**
     * Get the number of times that we have attempted and failed to upload this PendingFile.
     *
     * @return the number of times that we have attempted and failed to upload this PendingFile
     */
    public int getNumSendAttempts() {
        return mNumSendAttempts;
    }

    /**
     * Increment the number of send attempts.
     *
     * @return the number of times (post-increment) that we have attempted and failed to upload this PendingFile
     */
    public int incrementSendAttempts() {
        mNumSendAttempts++;
        return mNumSendAttempts;
    }
}
