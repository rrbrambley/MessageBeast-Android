package com.alwaysallthetime.adnlibutils.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.util.Log;

import com.alwaysallthetime.adnlib.AppDotNetClient;
import com.alwaysallthetime.adnlib.data.File;
import com.alwaysallthetime.adnlib.gson.AppDotNetGson;
import com.alwaysallthetime.adnlib.response.FileResponseHandler;
import com.alwaysallthetime.adnlibutils.ADNApplication;
import com.alwaysallthetime.adnlibutils.db.ADNDatabase;
import com.alwaysallthetime.adnlibutils.db.PendingFile;

import java.util.Set;
import java.util.UUID;

public class FileManager {

    private static final String TAG = "ADNLibUtils_FileManager";

    private final Context mContext;
    private final AppDotNetClient mClient;
    private ADNDatabase mDatabase;

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

        IntentFilter intentFilter = new IntentFilter(FileUploadService.INTENT_ACTION_FILE_UPLOAD_COMPLETE);
        mContext.registerReceiver(fileUploadReceiver, intentFilter);
    }

    public AppDotNetClient getClient() {
        return mClient;
    }

    public String addPendingFile(Uri uri, String type, String name, String mimeType, String kind, boolean isPublic) {
        String id = UUID.randomUUID().toString();
        mDatabase.insertOrReplacePendingFile(id, uri.toString(), type, name, mimeType, kind, isPublic, 0);
        return id;
    }

    public PendingFile getPendingFile(String id) {
        PendingFile pendingFile = mDatabase.getPendingFile(id);
        return pendingFile;
    }

    public void startPendingFileUpload(String pendingFileId) {
        Intent i = new Intent(mContext, FileUploadService.class);
        i.putExtra(FileUploadService.EXTRA_PENDING_FILE_ID, pendingFileId);
        mContext.startService(i);
    }

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
                    if(statusCode != null && statusCode >= 400 && statusCode < 500) {
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

                boolean success = intent.getBooleanExtra(FileUploadService.EXTRA_SUCCESS, false);
                if(success) {
                    //TODO - do something with the file.
                    String fileJson = intent.getStringExtra(FileUploadService.EXTRA_FILE);
                    File file = AppDotNetGson.getPersistenceInstance().fromJson(fileJson, File.class);

                    if(pendingFileId != null) {
                        mDatabase.deletePendingFile(pendingFileId);
                    }
                } else if(pendingFileId != null) {
                    Log.e(TAG, "Failed to upload pending file with id " + pendingFileId);
                    PendingFile pendingFile = mDatabase.getPendingFile(pendingFileId);
                    pendingFile.incrementSendAttempts();
                    mDatabase.insertOrReplacePendingFile(pendingFile);
                }
            }
        }
    };
}
