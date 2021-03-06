package com.alwaysallthetime.messagebeast.manager;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;

import com.alwaysallthetime.adnlib.AppDotNetClient;
import com.alwaysallthetime.adnlib.data.File;
import com.alwaysallthetime.adnlib.gson.AppDotNetGson;
import com.alwaysallthetime.adnlib.response.FileResponseHandler;
import com.alwaysallthetime.messagebeast.FileUtility;
import com.alwaysallthetime.messagebeast.db.PendingFile;

public class FileUploadService extends IntentService {

    private static final String TAG = "MessageBeast_FileUploadService";

    //register a broadcast receiver for this stuff
    public static final String INTENT_ACTION_FILE_UPLOAD_COMPLETE = "com.alwaysallthetime.messagebeast.manager.extras.FileUploadService.action.fileUpload";
    public static final String EXTRA_SUCCESS = "com.alwaysallthetime.messagebeast.manager.extras.FileUploadService.extras.success";
    public static final String EXTRA_FILE = "com.alwaysallthetime.messagebeast.manager.extras.FileUploadService.extras.file";
    public static final String EXTRA_FILE_NOT_FOUND = "com.alwaysallthetime.messagebeast.manager.extras.FileUploadService.extras.fileNotFound";

    //
    //start the service with either:
    //

    //1. These
    public static final String EXTRA_FILE_URI = "com.alwaysallthetime.messagebeast.manager.extras.FileUploadService.extras.bitmapUri";
    public static final String EXTRA_FILE_TYPE = "com.alwaysallthetime.messagebeast.manager.extras.FileUploadService.extras.fileType";
    public static final String EXTRA_FILENAME = "com.alwaysallthetime.messagebeast.manager.extras.FileUploadService.extras.filename";
    public static final String EXTRA_FILE_KIND = "com.alwaysallthetime.messagebeast.manager.extras.FileUploadService.extras.kind";
    public static final String EXTRA_PUBLIC = "com.alwaysallthetime.messagebeast.manager.extras.FileUploadService.extras.public";

    //
    //OR
    //

    //2. These:
    public static final String EXTRA_PENDING_FILE_ID = "com.alwaysallthetime.messagebeast.manager.extras.FileUploadService.extras.pendingFileId";
    public static final String EXTRA_ASSOCIATED_CHANNEL_ID = "com.alwaysallthetime.messagebeast.manager.extras.FileUploadService.extras.channelId";

    public FileUploadService() {
        super(FileUploadService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(intent.hasExtra(EXTRA_PENDING_FILE_ID)) {
            String pendingFileId = intent.getStringExtra(EXTRA_PENDING_FILE_ID);
            String channelId = intent.getStringExtra(EXTRA_ASSOCIATED_CHANNEL_ID);
            PendingFile pf = FileManager.getExistingInstance().getPendingFile(pendingFileId);
            if(pf != null) {
                byte[] fileBytes = FileUtility.getBytes(this, pf.getUri());
                if(fileBytes != null) {
                    createFile(fileBytes, pf.getType(), pf.getName(), pf.getMimeType(), pf.getKind(), pf.isPublic(), pendingFileId, channelId);
                } else {
                    sendFileNotFoundBroadcast(pendingFileId);
                }
            } else {
                sendFileNotFoundBroadcast(pendingFileId);
            }
        } else {
            Uri fileUri = intent.getParcelableExtra(EXTRA_FILE_URI);
            String fileType = intent.getStringExtra(EXTRA_FILE_TYPE);
            String filename = intent.getStringExtra(EXTRA_FILENAME);
            String fileKind = intent.getStringExtra(EXTRA_FILE_KIND);
            boolean isFilePublic = intent.getBooleanExtra(EXTRA_PUBLIC, false);

            String mimeType = FileUtility.getMimeType(this, fileUri);
            byte[] fileBytes = FileUtility.getBytes(this, fileUri);
            if(fileBytes != null) {
                createFile(fileBytes, fileType, filename, mimeType, fileKind, isFilePublic, null, null);
            } else {
                sendFileNotFoundBroadcast(null);
            }
        }
    }

    private void sendFileNotFoundBroadcast(String pendingFileId) {
        Intent i = new Intent(INTENT_ACTION_FILE_UPLOAD_COMPLETE);
        i.putExtra(EXTRA_SUCCESS, false);
        if(pendingFileId != null) {
            i.putExtra(EXTRA_PENDING_FILE_ID, pendingFileId);
        }
        i.putExtra(EXTRA_FILE_NOT_FOUND, true);
        sendBroadcast(i);
    }

    private void createFile(byte[] data, String fileType, String filename, String mimeType, String fileKind, boolean isPublic, final String pendingFileId, final String channelId) {
        AppDotNetClient client = FileManager.getExistingInstance().getClient();
        File file = new File(fileKind, fileType, filename, isPublic);
        client.createFile(file, data, mimeType, new FileResponseHandler() {
            @Override
            public void onSuccess(File responseData) {
                Intent i = new Intent(INTENT_ACTION_FILE_UPLOAD_COMPLETE);
                i.putExtra(EXTRA_SUCCESS, true);
                i.putExtra(EXTRA_FILE, AppDotNetGson.getPersistenceInstance().toJson(responseData));
                if(channelId != null) {
                    i.putExtra(EXTRA_ASSOCIATED_CHANNEL_ID, channelId);
                }
                if(pendingFileId != null) {
                    i.putExtra(EXTRA_PENDING_FILE_ID, pendingFileId);
                }
                sendBroadcast(i);
            }

            @Override
            public void onError(Exception error) {
                super.onError(error);

                Intent i = new Intent(INTENT_ACTION_FILE_UPLOAD_COMPLETE);
                i.putExtra(EXTRA_SUCCESS, false);
                if(channelId != null) {
                    i.putExtra(EXTRA_ASSOCIATED_CHANNEL_ID, channelId);
                }
                if(pendingFileId != null) {
                    i.putExtra(EXTRA_PENDING_FILE_ID, pendingFileId);
                }
                sendBroadcast(i);
            }
        });
    }
}
