package com.alwaysallthetime.adnlibutils.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ADNDatabaseOpenHelper extends SQLiteOpenHelper {

    private static final String TAG = "ADNLibUtils_AADNDatabaseOpenHelper";

    private static final String CREATE_MESSAGES_TABLE = "CREATE TABLE IF NOT EXISTS " + ADNDatabase.TABLE_MESSAGES + "(" +
            ADNDatabase.COL_MESSAGE_ID + " STRING PRIMARY KEY, " +
            ADNDatabase.COL_CHANNEL_ID + " STRING NOT NULL, " +
            ADNDatabase.COL_MESSAGE_DATE + " INTEGER NOT NULL, " +
            ADNDatabase.COL_MESSAGE_JSON + " STRING NOT NULL " +
            ")";

    public ADNDatabaseOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();

        try {
            db.execSQL(CREATE_MESSAGES_TABLE);
            db.setTransactionSuccessful();
        } catch(Exception exception) {
            Log.d(TAG, exception.getMessage(), exception);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
