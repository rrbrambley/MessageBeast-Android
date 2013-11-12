package com.alwaysallthetime.adnlibutils.manager;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.alwaysallthetime.adnlib.AppDotNetClient;
import com.alwaysallthetime.adnlib.data.File;
import com.alwaysallthetime.adnlib.gson.AppDotNetGson;
import com.alwaysallthetime.adnlib.response.FileResponseHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUploadService extends IntentService {

    private static final String TAG = "ADNLibUtils_FileUploadService";

    public static final String INTENT_ACTION_FILE_UPLOAD_COMPLETE = "com.alwaysallthetime.adnlibutils.manager.extras.FileUploadService.action.fileUpload";
    public static final String EXTRA_SUCCESS = "com.alwaysallthetime.adnlibutils.manager.extras.FileUploadService.extras.success";
    public static final String EXTRA_FILE = "com.alwaysallthetime.adnlibutils.manager.extras.FileUploadService.extras.file";

    public static final String EXTRA_FILE_URI = "com.alwaysallthetime.adnlibutils.manager.extras.FileUploadService.extras.bitmapUri";
    public static final String EXTRA_FILE_TYPE = "com.alwaysallthetime.adnlibutils.manager.extras.FileUploadService.extras.fileType";
    public static final String EXTRA_FILENAME = "com.alwaysallthetime.adnlibutils.manager.extras.FileUploadService.extras.filename";
    public static final String EXTRA_FILE_KIND = "com.alwaysallthetime.adnlibutils.manager.extras.FileUploadService.extras.kind";
    public static final String EXTRA_PUBLIC = "com.alwaysallthetime.adnlibutils.manager.extras.FileUploadService.extras.public";

    public FileUploadService() {
        super(FileUploadService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Uri fileUri = intent.getParcelableExtra(EXTRA_FILE_URI);
        String fileType = intent.getStringExtra(EXTRA_FILE_TYPE);
        String filename = intent.getStringExtra(EXTRA_FILENAME);
        String fileKind = intent.getStringExtra(EXTRA_FILE_KIND);
        boolean isFilePublic = intent.getBooleanExtra(EXTRA_PUBLIC, false);

        String mimeType = getMimeType(fileUri);
        byte[] fileBytes = getBytes(fileUri);

        MessageManager messageManager = MessageManager.getInstance();
        AppDotNetClient client = messageManager.getClient();

        File file = new File(fileKind, fileType, filename, isFilePublic);
        client.createFile(file, fileBytes, mimeType, new FileResponseHandler() {
            @Override
            public void onSuccess(File responseData) {
                Intent i = new Intent(INTENT_ACTION_FILE_UPLOAD_COMPLETE);
                i.putExtra(EXTRA_SUCCESS, true);
                i.putExtra(EXTRA_FILE, AppDotNetGson.getPersistenceInstance().toJson(responseData));
                sendBroadcast(i);
            }

            @Override
            public void onError(Exception error) {
                super.onError(error);

                Intent i = new Intent(INTENT_ACTION_FILE_UPLOAD_COMPLETE);
                i.putExtra(EXTRA_SUCCESS, false);
                sendBroadcast(i);
            }
        });
    }

    public String getMimeType(Uri fileUri) {
        String mimeType = getContentResolver().getType(fileUri);
        if(mimeType != null) {
            return mimeType;
        } else {
            String type = null;
            String uri = fileUri.toString();
            String extension = MimeTypeMap.getFileExtensionFromUrl(uri);
            if (extension != null) {
                MimeTypeMap mime = MimeTypeMap.getSingleton();
                type = mime.getMimeTypeFromExtension(extension);
            }
            return type;
        }
    }

    private byte[] getBytes(Uri uri) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream inputStream = null;
        try {
            ContentResolver cr = getBaseContext().getContentResolver();
            inputStream = cr.openInputStream(uri);
            byte[] buf = new byte[1024];
            int n;
            while (-1 != (n = inputStream.read(buf))) {
                baos.write(buf, 0, n);
            }
        } catch(Exception e) {
            Log.d(TAG, e.getMessage(), e);
        } finally {
            if(inputStream != null) {
                try {
                    inputStream.close();
                } catch(IOException e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            }
        }
        return baos.toByteArray();
    }
}
