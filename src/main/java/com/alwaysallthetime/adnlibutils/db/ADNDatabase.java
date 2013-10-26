package com.alwaysallthetime.adnlibutils.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.alwaysallthetime.adnlib.data.Entities;
import com.alwaysallthetime.adnlib.data.Message;
import com.alwaysallthetime.adnlib.gson.AppDotNetGson;
import com.alwaysallthetime.adnlibutils.MessagePlus;
import com.alwaysallthetime.adnlibutils.manager.MinMaxPair;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ADNDatabase {

    private static final String TAG = "ADNLibUtils_AADNDatabase";
    private static final String DB_NAME = "aadndatabase.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_MESSAGES = "messages";
    public static final String COL_MESSAGE_ID = "message_id";
    public static final String COL_MESSAGE_CHANNEL_ID = "message_channel_id";
    public static final String COL_MESSAGE_DATE = "message_date";
    public static final String COL_MESSAGE_JSON = "message_json";

    public static final String TABLE_HASHTAGS = "hashtags";
    public static final String COL_HASHTAG_NAME = "hashtag_name";
    public static final String COL_HASHTAG_MESSAGE_ID = "hashtag_message_id";
    public static final String COL_HASHTAG_CHANNEL_ID = "hashtag_channel_id";
    public static final String COL_HASHTAG_DATE = "hashtag_date";

    private static final String INSERT_OR_REPLACE_MESSAGE = "INSERT OR REPLACE INTO " + TABLE_MESSAGES + " " +
            "(" +
            COL_MESSAGE_ID + ", " +
            COL_MESSAGE_CHANNEL_ID + ", " +
            COL_MESSAGE_DATE + ", " +
            COL_MESSAGE_JSON +
            ") " +
            "VALUES(?, ?, ?, ?)";

    private static final String INSERT_OR_REPLACE_HASHTAG = "INSERT OR REPLACE INTO " + TABLE_HASHTAGS + " " +
            "(" +
            COL_HASHTAG_NAME + ", " +
            COL_HASHTAG_MESSAGE_ID + ", " +
            COL_HASHTAG_CHANNEL_ID + ", " +
            COL_HASHTAG_DATE +
            ") " +
            "VALUES(?, ?, ?, ?)";

    private static ADNDatabase sInstance;

    private SQLiteDatabase mDatabase;
    private SQLiteStatement mInsertOrReplaceMessage;
    private SQLiteStatement mInsertOrReplaceHashtag;
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

    public Map<String, HashtagInstances> insertOrReplaceHashtags(MessagePlus message) {
        HashMap<String, HashtagInstances> instances = new HashMap<String, HashtagInstances>();
        if(mInsertOrReplaceHashtag == null) {
            mInsertOrReplaceHashtag = mDatabase.compileStatement(INSERT_OR_REPLACE_HASHTAG);
        }
        Message m = message.getMessage();
        ArrayList<Entities.Hashtag> hashtags = m.getEntities().getHashtags();
        mDatabase.beginTransaction();
        try {
            for(Entities.Hashtag h : hashtags) {
                String name = h.getName();
                String messageId = m.getId();
                mInsertOrReplaceHashtag.bindString(1, name);
                mInsertOrReplaceHashtag.bindString(2, messageId);
                mInsertOrReplaceHashtag.bindString(3, m.getChannelId());
                mInsertOrReplaceHashtag.bindLong(4, message.getDisplayDate().getTime());
                mInsertOrReplaceHashtag.execute();

                HashtagInstances hashtagInstances = instances.get(name);
                if(hashtagInstances == null) {
                    hashtagInstances = new HashtagInstances(name, messageId);
                    instances.put(name, hashtagInstances);
                } else {
                    hashtagInstances.addInstance(messageId);
                }
            }
            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
            mInsertOrReplaceHashtag.clearBindings();
        }
        return instances;
    }

    /**
     * Get all Hashtags for a channel whose associated messages were created since a specified date.
     *
     * @param channelId The id of the channel
     * @param sinceDate The earliest date for which hashtags should be returned.
     * @return a Map whose keys are hashtag names, mapped to HashtagInstances
     */
    public Map<String, HashtagInstances> getHashtags(String channelId, Date sinceDate) {
        return getHashtags(channelId, null, sinceDate);
    }

    /**
     * Get all Hashtags for a channel whose associated messages were created within the provided
     * date window, i.e. (beforeDate, sinceDate]
     *
     * @param channelId The id of the channel
     * @param beforeDate The date before which all hashtags' associated messages were created. Can be null.
     * @param sinceDate The earliest date for which hashtags should be returned. May not be null.
     * @return a Map whose keys are hashtag names, mapped to HashtagInstances
     *
     * @see com.alwaysallthetime.adnlibutils.db.ADNDatabase#getHashtags(String, java.util.Date)
     */
    public Map<String, HashtagInstances> getHashtags(String channelId, Date beforeDate, Date sinceDate) {
        HashMap<String, HashtagInstances> instances = new HashMap<String, HashtagInstances>();
        Cursor cursor = null;
        try {
            String[] args = null;
            String where = COL_HASHTAG_CHANNEL_ID + " =? AND " + COL_HASHTAG_DATE + " >= ?";
            if(beforeDate != null) {
                where += " AND " + COL_HASHTAG_DATE + " < ?";
                args = new String[] { channelId,  String.valueOf(sinceDate.getTime()), String.valueOf(beforeDate.getTime()) };
            } else {
                args = new String[] { channelId,  String.valueOf(sinceDate.getTime()) };
            }
            cursor = mDatabase.query(TABLE_HASHTAGS, null, where, args, null, null, null, null);

            if(cursor.moveToNext()) {
                do {
                    String hashtag = cursor.getString(0);
                    String messageId = cursor.getString(1);
                    HashtagInstances c = instances.get(hashtag);
                    if(c == null) {
                        c = new HashtagInstances(hashtag, messageId);
                        instances.put(hashtag, c);
                    } else {
                        c.addInstance(messageId);
                    }
                } while(cursor.moveToNext());
            }
        } catch(Exception e) {
            Log.d(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return instances;
    }

    /**
     * Get a HashtagInstances object representing all Messages in whichh the specified
     * hashtag was used.
     *
     * @param channelId The id of the channel
     * @param hashtagName the hashtag for which instances should be retrieved.
     *
     * @return HashtagInstances
     */
    public HashtagInstances getHashtagInstances(String channelId, String hashtagName) {
        Cursor cursor = null;
        HashtagInstances instances = new HashtagInstances(hashtagName);
        try {
            String where = COL_HASHTAG_CHANNEL_ID + " =? AND " + COL_HASHTAG_NAME + " = ?";
            String[] args = new String[] { channelId, hashtagName };
            cursor = mDatabase.query(TABLE_HASHTAGS, null, where, args, null, null, null, null);

            if(cursor.moveToNext()) {
                do {
                    String messageId = cursor.getString(1);
                    instances.addInstance(messageId);
                } while(cursor.moveToNext());
            }
        } catch(Exception e) {
            Log.d(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return instances;
    }

    /**
     * Get Messages in a Channel
     *
     * @param channelId The id of the Channel.
     * @param messageIds The ids of the Messages to get.
     * @return OrderedMessageBatch
     */
    public OrderedMessageBatch getMessages(String channelId, Collection<String> messageIds) {
        LinkedHashMap<String, MessagePlus> messages = new LinkedHashMap<String, MessagePlus>();
        String maxId = null;
        String minId = null;
        Cursor cursor = null;
        try {
            String where = COL_MESSAGE_CHANNEL_ID + " =? AND " + COL_MESSAGE_ID + " IN (";
            String[] args = new String[messageIds.size() + 1];
            args[0] = channelId;

            int index = 1;
            Iterator<String> iterator = messageIds.iterator();
            while(iterator.hasNext()) {
                args[index] = iterator.next();
                if(index > 1) {
                    where += ", ?";
                } else {
                    where += " ?";
                }
                index++;
            }
            where += ")";
            String orderBy = COL_MESSAGE_DATE + " DESC";
            cursor = mDatabase.query(TABLE_MESSAGES, null, where, args, null, null, orderBy, null);

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

    public OrderedMessageBatch getMessages(String channelId, int limit) {
        return getMessages(channelId, null, limit);
    }

    public OrderedMessageBatch getMessages(String channelId, Date beforeDate, int limit) {
        LinkedHashMap<String, MessagePlus> messages = new LinkedHashMap<String, MessagePlus>();
        String maxId = null;
        String minId = null;
        Cursor cursor = null;
        try {
            String where = COL_MESSAGE_CHANNEL_ID + " =?";
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

    public void deleteMessage(Message message) {
        mDatabase.beginTransaction();

        try {
            mDatabase.delete(TABLE_MESSAGES, COL_MESSAGE_ID + " = '" + message.getId() + "'", null);

            ArrayList<Entities.Hashtag> hashtags = message.getEntities().getHashtags();
            for(Entities.Hashtag h : hashtags) {
                String where = COL_HASHTAG_NAME + " = '" + h.getName() + "' AND " + COL_HASHTAG_MESSAGE_ID + " = '" + message.getId() + "'";
                mDatabase.delete(TABLE_HASHTAGS, where, null);
            }
            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
        }
    }

    public void deleteMessages(String channelId) {
        mDatabase.beginTransaction();

        try {
            mDatabase.delete(TABLE_MESSAGES, COL_MESSAGE_CHANNEL_ID + " = '" + channelId + "'", null);
            mDatabase.delete(TABLE_HASHTAGS, COL_HASHTAG_CHANNEL_ID + " = '" + channelId + "'", null);
            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
        }
    }
}
