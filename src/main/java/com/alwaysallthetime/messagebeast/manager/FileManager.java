package com.alwaysallthetime.messagebeast.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.util.Log;

import com.alwaysallthetime.adnlib.AppDotNetClient;
import com.alwaysallthetime.adnlib.data.File;
import com.alwaysallthetime.adnlib.response.FileResponseHandler;
import com.alwaysallthetime.messagebeast.ADNApplication;
import com.alwaysallthetime.messagebeast.db.ADNDatabase;
import com.alwaysallthetime.messagebeast.db.PendingFile;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The FileManager is used to handle the creation and deletion of App.net File objects
 */
public class FileManager {

    private static final String TAG = "MessageBeast_FileManager";

    private final Context mContext;
    private final AppDotNetClient mClient;
    private ADNDatabase mDatabase;
    private Set<String> mFilesInProgress;

    private static FileManager sInstance;

    /**
     * Get an instance of the existing FileManager.
     *
     * To be used only after getInstance(AppDotNetClient) has been called one
     * or more times.
     *
     * @return the existing instance of the FileManager
     */
    public static FileManager getExistingInstance() {
        return sInstance;
    }

    /**
     * Get an instance of FileManager.
     *
     * @param client the AppDotNetClient to use with the FileManager if a new one is created. If
     *               the singleton already exists, then this is ignored.
     * @return FileManager
     */
    public static FileManager getInstance(AppDotNetClient client) {
        if(sInstance == null) {
            sInstance = new FileManager(client);
        }
        return sInstance;
    }

    private FileManager(AppDotNetClient client) {
        mClient = client;
        mContext = ADNApplication.getContext();
        mDatabase = ADNDatabase.getInstance(mContext);
        mFilesInProgress = Collections.synchronizedSet(new HashSet<String>(1));

        IntentFilter intentFilter = new IntentFilter(FileUploadService.INTENT_ACTION_FILE_UPLOAD_COMPLETE);
        mContext.registerReceiver(fileUploadReceiver, intentFilter);
    }

    /**
     * Get the AppDotNetClient used by this FileManager.
     *
     * @return the AppDotNetClient used by this FileManager
     */
    public AppDotNetClient getClient() {
        return mClient;
    }

    /**
     * Create a new PendingFile to be uploaded to App.net at some point in the future. The PendingFile
     * is persisted to sqlite.
     *
     * @param uri the Uri corresponding to the location of the file on disk
     * @param type the type to be set on the App.net File's "type" field.
     * @param name the name of the App.net File
     * @param mimeType the MIME type of the file
     * @param kind the kind of the App.net file
     * @param isPublic true if the File is public, false otherwise
     * @return a new PendingFile
     */
    public PendingFile createPendingFile(Uri uri, String type, String name, String mimeType, String kind, boolean isPublic) {
        String id = UUID.randomUUID().toString();
        mDatabase.insertOrReplacePendingFile(id, uri.toString(), type, name, mimeType, kind, isPublic, 0);
        return new PendingFile(id, uri, type, name, mimeType, kind, isPublic, 0);
    }

    /**
     * Get a persisted PendingFile.
     *
     * @param id the id of the PendingFile
     * @return a PendingFile with the provided id, or null if none exists.
     */
    public PendingFile getPendingFile(String id) {
        PendingFile pendingFile = mDatabase.getPendingFile(id);
        return pendingFile;
    }

    /**
     * Start uploading a PendingFile. If the file upload is already in progress, then this is a
     * no-op.
     *
     * @param pendingFileId the id of the PendingFile
     *
     * @see com.alwaysallthetime.messagebeast.manager.FileManager#startPendingFileUpload(String, String)
     */
    public void startPendingFileUpload(String pendingFileId) {
        startPendingFileUpload(pendingFileId, null);
    }

    /**
     * Start uploading a PendingFile. If the file upload is already in progress, then this is a
     * no-op.
     *
     * @param pendingFileId the id of the PendingFile
     * @param associatedChannelId the id of a Channel associated with the file, or null if none exists.
     * For example, if the PendingFile is used for a PendingFileAttachment on a Message, then the
     * id of the Channel that contains the Message should be provided. This serves as a hint to
     * FileUploadService.INTENT_ACTION_FILE_UPLOAD_COMPLETE broadcast receivers why the file upload
     * may have been initiated (e.g. need to upload this file before we can create a new Message
     * with an OEmbed.
     */
    public void startPendingFileUpload(String pendingFileId, String associatedChannelId) {
        if(!mFilesInProgress.contains(pendingFileId)) {
            mFilesInProgress.add(pendingFileId);
            Intent i = new Intent(mContext, FileUploadService.class);
            i.putExtra(FileUploadService.EXTRA_PENDING_FILE_ID, pendingFileId);
            i.putExtra(FileUploadService.EXTRA_ASSOCIATED_CHANNEL_ID, associatedChannelId);
            mContext.startService(i);
        }
    }

    /**
     * Start uploading a file.
     *
     * @param uri the Uri corresponding to the location of the file on disk
     * @param type the type to be set on the App.net File's "type" field.
     * @param name the name of the App.net File
     * @param kind the kind of the App.net file
     * @param isPublic true if the File is public, false otherwise
     */
    public void startFileUpload(Uri uri, String type, String name, String kind, boolean isPublic) {
        Intent i = new Intent(mContext, FileUploadService.class);
        i.putExtra(FileUploadService.EXTRA_FILE_URI, uri);
        i.putExtra(FileUploadService.EXTRA_FILE_TYPE, type);
        i.putExtra(FileUploadService.EXTRA_FILENAME, name);
        i.putExtra(FileUploadService.EXTRA_FILE_KIND, kind);
        i.putExtra(FileUploadService.EXTRA_PUBLIC, isPublic);
        mContext.startService(i);
    }

    /**
     * Send all pending file deletions in all Channels.
     */
    public void sendPendingFileDeletions() {
        Set<String> pendingFileDeletions = mDatabase.getPendingFileDeletions();
        for(final String fileId : pendingFileDeletions) {
            mClient.deleteFile(fileId, new FileResponseHandler() {
                @Override
                public void onSuccess(File responseData) {
                    mDatabase.deletePendingFileDeletion(responseData.getId());
                }

                @Override
                public void onError(Exception error) {
                    super.onError(error);
                    Integer statusCode = getStatusCode();
                    if(statusCode != null && statusCode == 403) {
                        mDatabase.deletePendingFileDeletion(fileId);
                    }
                }
            });
        }
    }

    private final BroadcastReceiver fileUploadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(FileUploadService.INTENT_ACTION_FILE_UPLOAD_COMPLETE.equals(intent.getAction())) {
                //this can be null.
                String pendingFileId = intent.getStringExtra(FileUploadService.EXTRA_PENDING_FILE_ID);
                if(pendingFileId != null) {
                    mFilesInProgress.remove(pendingFileId);
                }

                boolean success = intent.getBooleanExtra(FileUploadService.EXTRA_SUCCESS, false);
                if(success) {
                    if(pendingFileId != null) {
                        mDatabase.deletePendingFile(pendingFileId);
                    }
                } else if(pendingFileId != null) {
                    Log.e(TAG, "Failed to upload pending file with id " + pendingFileId);
                    PendingFile pendingFile = mDatabase.getPendingFile(pendingFileId);
                    if(pendingFile != null) {
                        pendingFile.incrementSendAttempts();
                        mDatabase.insertOrReplacePendingFile(pendingFile);
                    } else {
                        Log.e(TAG, "File " + pendingFileId + " is not in the database");
                    }
                }
            }
        }
    };
}
