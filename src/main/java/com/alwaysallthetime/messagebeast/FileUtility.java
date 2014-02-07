package com.alwaysallthetime.messagebeast;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtility {
    private static final String TAG = "MessageBeast_FileUtility";

    public static String getMimeType(Context context, Uri fileUri) {
        String mimeType = context.getContentResolver().getType(fileUri);
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

    public static byte[] getBytes(Context context, Uri uri) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream inputStream = null;
        try {
            ContentResolver cr = context.getContentResolver();
            inputStream = cr.openInputStream(uri);
            byte[] buf = new byte[1024];
            int n;
            while (-1 != (n = inputStream.read(buf))) {
                baos.write(buf, 0, n);
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(inputStream != null) {
                try {
                    inputStream.close();
                } catch(IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }
        return baos != null ? baos.toByteArray() : null;
    }
}
