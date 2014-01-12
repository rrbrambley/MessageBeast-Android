package com.alwaysallthetime.adnlibutils.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;

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
        for(String fileId : pendingFileDeletions) {
            mClient.deleteFile(fileId, new FileResponseHandler() {
                @Override
                public void onSuccess(File responseData) {
                    mDatabase.deletePendingFileDeletion(responseData.getId());
                }
            });
        }
    }

    private final BroadcastReceiver fileUploadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(FileUploadService.INTENT_ACTION_FILE_UPLOAD_COMPLETE.equals(intent.getAction())) {
                boolean success = intent.getBooleanExtra(FileUploadService.EXTRA_SUCCESS, false);
                if(success) {
                    //TODO - do something with the file.
                    String fileJson = intent.getStringExtra(FileUploadService.EXTRA_FILE);
                    File file = AppDotNetGson.getPersistenceInstance().fromJson(fileJson, File.class);

                    if(intent.hasExtra(FileUploadService.EXTRA_PENDING_FILE_ID)) {
                        mDatabase.deletePendingFile(intent.getStringExtra(FileUploadService.EXTRA_PENDING_FILE_ID));
                    }
                } else {
                    //TODO
                }
            }
        }
    };
}
