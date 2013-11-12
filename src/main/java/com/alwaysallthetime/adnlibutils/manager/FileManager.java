package com.alwaysallthetime.adnlibutils.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;

import com.alwaysallthetime.adnlib.AppDotNetClient;
import com.alwaysallthetime.adnlib.data.File;
import com.alwaysallthetime.adnlib.gson.AppDotNetGson;
import com.alwaysallthetime.adnlibutils.db.ADNDatabase;
import com.alwaysallthetime.adnlibutils.db.PendingFile;

import java.util.UUID;

public class FileManager {

    private final Context mContext;
    private ADNDatabase mDatabase;
    private AppDotNetClient mClient;

    private static FileManager sInstance;

    public static FileManager getInstance() {
        return sInstance;
    }

    public static FileManager getInstance(Context context, AppDotNetClient client) {
        if(sInstance == null) {
            sInstance = new FileManager(context, client);
        }
        return sInstance;
    }

    private FileManager(Context context, AppDotNetClient client) {
        mContext = context;
        mDatabase = ADNDatabase.getInstance(context);
        mClient = client;

        IntentFilter intentFilter = new IntentFilter(FileUploadService.INTENT_ACTION_FILE_UPLOAD_COMPLETE);
        context.registerReceiver(fileUploadReceiver, intentFilter);
    }

    public String addPendingFile(Uri uri, String type, String name, String mimeType, String kind, boolean isPublic) {
        String id = UUID.randomUUID().toString();
        mDatabase.insertOrReplacePendingFile(id, uri.toString(), type, name, mimeType, kind, isPublic);
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
