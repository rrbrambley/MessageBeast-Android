package com.alwaysallthetime.adnlibutils.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.alwaysallthetime.adnlib.data.Message;
import com.alwaysallthetime.adnlib.gson.AppDotNetGson;
import com.alwaysallthetime.adnlibutils.MessagePlus;
import com.alwaysallthetime.adnlibutils.manager.MinMaxPair;
import com.google.gson.Gson;

import java.util.Date;
import java.util.LinkedHashMap;

public class ADNDatabase {

    private static final String TAG = "ADNLibUtils_AADNDatabase";
    private static final String DB_NAME = "aadndatabase.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_MESSAGES = "messages";
    public static final String COL_MESSAGE_ID = "message_id";
    public static final String COL_CHANNEL_ID = "message_channel_id";
    public static final String COL_MESSAGE_DATE = "message_date";
    public static final String COL_MESSAGE_JSON = "message_json";

    private static final String INSERT_OR_REPLACE_MESSAGE = "INSERT OR REPLACE INTO " + TABLE_MESSAGES + " " +
            "(" +
            COL_MESSAGE_ID + ", " +
            COL_CHANNEL_ID + ", " +
            COL_MESSAGE_DATE + ", " +
            COL_MESSAGE_JSON +
            ") " +
            "VALUES(?, ?, ?, ?)";

    private static ADNDatabase sInstance;

    private SQLiteDatabase mDatabase;
    private SQLiteStatement mInsertOrReplaceMessage;
    private Gson mGson;

    public static synchronized ADNDatabase getInstance(Context context) {
        if(sInstance == null) {
            sInstance = new ADNDatabase(context);
        }
        return sInstance;
    }

    private ADNDatabase(Context context) {
        ADNDatabaseOpenHelper openHelper = new ADNDatabaseOpenHelper(context, DB_NAME, null, DB_VERSION);
        mDatabase = openHelper.getWritableDatabase();
        mGson = AppDotNetGson.getPersistenceInstance();
    }

    public void insertOrReplaceMessage(MessagePlus messagePlus) {
        if(mInsertOrReplaceMessage == null) {
            mInsertOrReplaceMessage = mDatabase.compileStatement(INSERT_OR_REPLACE_MESSAGE);
        }
        mDatabase.beginTransaction();

        try {
            Message message = messagePlus.getMessage();
            Date displayDate = messagePlus.getDisplayDate();
            mInsertOrReplaceMessage.bindString(1, message.getId());
            mInsertOrReplaceMessage.bindString(2, message.getChannelId());
            mInsertOrReplaceMessage.bindLong(3, displayDate.getTime());
            mInsertOrReplaceMessage.bindString(4, mGson.toJson(message));
            mInsertOrReplaceMessage.execute();

            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
            mInsertOrReplaceMessage.clearBindings();
        }
    }

    public OrderedMessageBatch getMessages(String channelId, int limit) {
        return getMessages(channelId, null, limit);
    }

    public OrderedMessageBatch getMessages(String channelId, Date beforeDate, int limit) {
        LinkedHashMap<String, MessagePlus> messages = new LinkedHashMap<String, MessagePlus>();
        String maxId = null;
        String minId = null;
        Cursor cursor = null;
        try {
            String where = COL_CHANNEL_ID + " =?";
            if(beforeDate != null) {
                where += " AND " + COL_MESSAGE_DATE + " < ?";
            }
            String[] args = null;
            if(beforeDate != null) {
                args = new String[] { channelId,  String.valueOf(beforeDate.getTime()) };
            } else {
                args = new String[] { channelId };
            }
            String orderBy = COL_MESSAGE_DATE + " DESC";
            cursor = mDatabase.query(TABLE_MESSAGES, null, where, args, null, null, orderBy, String.valueOf(limit));

            Message message = null;
            if(cursor.moveToNext()) {
                do {
                    String messageId = cursor.getString(0);
                    long date = cursor.getLong(2);
                    String messageJson = cursor.getString(3);
                    message = mGson.fromJson(messageJson, Message.class);

                    MessagePlus messagePlus = new MessagePlus(message);
                    messagePlus.setDisplayDate(new Date(date));
                    messages.put(messageId, messagePlus);

                    if(maxId == null) {
                        maxId = messageId;
                    }
                } while(cursor.moveToNext());

                if(message != null) {
                    minId = message.getId();
                }
            }
        } catch(Exception e) {
            Log.d(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return new OrderedMessageBatch(messages, new MinMaxPair(minId, maxId));
    }
}
