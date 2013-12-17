package com.alwaysallthetime.adnlibutils.db;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.util.Log;

import com.alwaysallthetime.adnlib.data.Entities;
import com.alwaysallthetime.adnlib.data.Message;
import com.alwaysallthetime.adnlib.gson.AppDotNetGson;
import com.alwaysallthetime.adnlibutils.manager.MinMaxPair;
import com.alwaysallthetime.adnlibutils.model.DisplayLocation;
import com.alwaysallthetime.adnlibutils.model.Geolocation;
import com.alwaysallthetime.adnlibutils.model.MessagePlus;
import com.google.gson.Gson;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ADNDatabase {

    private static final String TAG = "ADNLibUtils_AADNDatabase";
    private static final String DB_NAME = "aadndatabase.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_MESSAGES = "messages";
    public static final String COL_MESSAGE_ID = "message_id";
    public static final String COL_MESSAGE_CHANNEL_ID = "message_channel_id";
    public static final String COL_MESSAGE_DATE = "message_date";
    public static final String COL_MESSAGE_JSON = "message_json";
    public static final String COL_MESSAGE_TEXT = "message_text";
    public static final String COL_MESSAGE_UNSENT = "message_unsent";
    public static final String COL_MESSAGE_SEND_ATTEMPTS = "message_send_attempts";

    public static final String TABLE_HASHTAG_INSTANCES = "hashtags";
    public static final String COL_HASHTAG_INSTANCE_NAME = "hashtag_name";
    public static final String COL_HASHTAG_INSTANCE_MESSAGE_ID = "hashtag_message_id";
    public static final String COL_HASHTAG_INSTANCE_CHANNEL_ID = "hashtag_channel_id";
    public static final String COL_HASHTAG_INSTANCE_DATE = "hashtag_date";

    public static final String TABLE_GEOLOCATIONS = "geolocations";
    public static final String COL_GEOLOCATION_LOCALITY = "geolocation_locality";
    public static final String COL_GEOLOCATION_SUBLOCALITY = "geolocation_sublocality";
    public static final String COL_GEOLOCATION_LATITUDE = "geolocation_latitude";
    public static final String COL_GEOLOCATION_LONGITUDE = "geolocation_longitude";

    public static final String TABLE_LOCATION_INSTANCES = "locations";
    public static final String COL_LOCATION_INSTANCE_NAME = "location_name";
    public static final String COL_LOCATION_INSTANCE_SHORT_NAME = "location_short_name";
    public static final String COL_LOCATION_INSTANCE_MESSAGE_ID = "location_message_id";
    public static final String COL_LOCATION_INSTANCE_CHANNEL_ID = "location_channel_id";
    public static final String COL_LOCATION_INSTANCE_FACTUAL_ID = "location_factual_id";
    public static final String COL_LOCATION_INSTANCE_LATITUDE = "location_latitude";
    public static final String COL_LOCATION_INSTANCE_LONGITUDE = "location_longitude";
    public static final String COL_LOCATION_INSTANCE_DATE = "location_date";

    public static final String TABLE_OEMBED_INSTANCES = "oembeds";
    public static final String COL_OEMBED_INSTANCE_TYPE = "oembed_type";
    public static final String COL_OEMBED_INSTANCE_MESSAGE_ID = "oembed_instance_message_id";
    public static final String COL_OEMBED_INSTANCE_CHANNEL_ID = "oembed_instance_channel_id";
    public static final String COL_OEMBED_INSTANCE_COUNT = "oembed_instance_count";
    public static final String COL_OEMBED_INSTANCE_DATE = "oembed_instance_date";

    public static final String TABLE_PENDING_FILES = "pending_files";
    public static final String COL_PENDING_FILE_ID = "pending_file_id";
    public static final String COL_PENDING_FILE_URI = "pending_file_uri";
    public static final String COL_PENDING_FILE_TYPE = "pending_file_type";
    public static final String COL_PENDING_FILE_NAME = "pending_file_name";
    public static final String COL_PENDING_FILE_MIMETYPE = "pending_file_mimetype";
    public static final String COL_PENDING_FILE_KIND = "pending_file_kind";
    public static final String COL_PENDING_FILE_PUBLIC = "pending_file_public";
    public static final String COL_PENDING_FILE_SEND_ATTEMPTS = "pending_file_send_attempts";

    public static final String TABLE_PENDING_OEMBEDS = "pending_oembeds";
    public static final String COL_PENDING_OEMBED_PENDING_FILE_ID = "pending_oembed_file_id";
    public static final String COL_PENDING_OEMBED_MESSAGE_ID = "pending_oembed_message_id";
    public static final String COL_PENDING_OEMBED_CHANNEL_ID = "pending_oembed_channel_id";

    public static final String TABLE_PENDING_MESSAGE_DELETIONS = "pending_message_deletions";
    public static final String COL_PENDING_MESSAGE_DELETION_MESSAGE_ID = "pending_message_deletion_message_id";
    public static final String COL_PENDING_MESSAGE_DELETION_CHANNEL_ID = "pending_message_deletion_channel_id";
    public static final String COL_PENDING_MESSAGE_DELETION_DELETE_ASSOCIATED_FILES = "pending_message_deletion_delete_files";

    public static final String TABLE_ACTION_MESSAGES = "action_messages";
    public static final String COL_ACTION_MESSAGE_ID = "action_message_id";
    public static final String COL_ACTION_MESSAGE_CHANNEL_ID = "action_channel_id";
    public static final String COL_ACTION_MESSAGE_TARGET_MESSAGE_ID = "action_target_message_id";
    public static final String COL_ACTION_MESSAGE_TARGET_CHANNEL_ID = "action_target_channel_id";

    /**
     * Precision values to be used when retrieving location instances.
     *
     * These values are approximate.
     */
    public enum LocationPrecision {
        ONE_HUNDRED_METERS, //actually 111 m
        ONE_THOUSAND_METERS, //actually 1.11 km
        TEN_THOUSAND_METERS //actually 11.1 km
    };

    private static final String INSERT_OR_REPLACE_MESSAGE = "INSERT OR REPLACE INTO " + TABLE_MESSAGES +
            " (" +
            COL_MESSAGE_ID + ", " +
            COL_MESSAGE_CHANNEL_ID + ", " +
            COL_MESSAGE_DATE + ", " +
            COL_MESSAGE_JSON + ", " +
            COL_MESSAGE_TEXT + ", " +
            COL_MESSAGE_UNSENT + ", " +
            COL_MESSAGE_SEND_ATTEMPTS +
            ") " +
            "VALUES(?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_OR_REPLACE_HASHTAG = "INSERT OR REPLACE INTO " + TABLE_HASHTAG_INSTANCES +
            " (" +
            COL_HASHTAG_INSTANCE_NAME + ", " +
            COL_HASHTAG_INSTANCE_MESSAGE_ID + ", " +
            COL_HASHTAG_INSTANCE_CHANNEL_ID + ", " +
            COL_HASHTAG_INSTANCE_DATE +
            ") " +
            "VALUES(?, ?, ?, ?)";

    private static final String INSERT_OR_REPLACE_GEOLOCATION = "INSERT OR REPLACE INTO " + TABLE_GEOLOCATIONS +
            " (" +
            COL_GEOLOCATION_LOCALITY + ", " +
            COL_GEOLOCATION_SUBLOCALITY + ", " +
            COL_GEOLOCATION_LATITUDE + ", " +
            COL_GEOLOCATION_LONGITUDE +
            ") " +
            "VALUES(?, ?, ?, ?)";

    private static final String INSERT_OR_REPLACE_LOCATION_INSTANCE = "INSERT OR REPLACE INTO " + TABLE_LOCATION_INSTANCES +
            " (" +
            COL_LOCATION_INSTANCE_NAME + ", " +
            COL_LOCATION_INSTANCE_SHORT_NAME + ", " +
            COL_LOCATION_INSTANCE_MESSAGE_ID + ", " +
            COL_LOCATION_INSTANCE_CHANNEL_ID + ", " +
            COL_LOCATION_INSTANCE_LATITUDE + ", " +
            COL_LOCATION_INSTANCE_LONGITUDE + ", " +
            COL_LOCATION_INSTANCE_FACTUAL_ID + ", " +
            COL_LOCATION_INSTANCE_DATE +
            ") " +
            "VALUES(?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_OR_REPLACE_OEMBED_INSTANCE = "INSERT OR REPLACE INTO " + TABLE_OEMBED_INSTANCES +
            " (" +
            COL_OEMBED_INSTANCE_TYPE + ", " +
            COL_OEMBED_INSTANCE_MESSAGE_ID + ", " +
            COL_OEMBED_INSTANCE_CHANNEL_ID + ", " +
            COL_OEMBED_INSTANCE_COUNT + ", " +
            COL_OEMBED_INSTANCE_DATE +
            ") " +
            "VALUES(?, ?, ?, ?, ?)";

    private static final String INSERT_OR_REPLACE_PENDING_FILE = "INSERT OR REPLACE INTO " + TABLE_PENDING_FILES +
            " (" +
            COL_PENDING_FILE_ID + ", " +
            COL_PENDING_FILE_URI + ", " +
            COL_PENDING_FILE_TYPE + ", " +
            COL_PENDING_FILE_NAME + ", " +
            COL_PENDING_FILE_MIMETYPE + ", " +
            COL_PENDING_FILE_KIND + ", " +
            COL_PENDING_FILE_PUBLIC + ", " +
            COL_PENDING_FILE_SEND_ATTEMPTS +
            ") " +
            "VALUES(?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_OR_REPLACE_PENDING_MESSAGE_DELETION = "INSERT OR REPLACE INTO " + TABLE_PENDING_MESSAGE_DELETIONS +
            " (" +
            COL_PENDING_MESSAGE_DELETION_MESSAGE_ID + ", " +
            COL_PENDING_MESSAGE_DELETION_CHANNEL_ID + ", " +
            COL_PENDING_MESSAGE_DELETION_DELETE_ASSOCIATED_FILES +
            ") " +
            "VALUES(?, ?, ?)";

    private static final String INSERT_OR_REPLACE_PENDING_OEMBED = "INSERT OR REPLACE INTO " + TABLE_PENDING_OEMBEDS +
            " (" +
            COL_PENDING_OEMBED_PENDING_FILE_ID + ", " +
            COL_PENDING_OEMBED_MESSAGE_ID + ", " +
            COL_PENDING_OEMBED_CHANNEL_ID +
            ") " +
            "VALUES(?, ?, ?)";

    private static final String INSERT_OR_REPLACE_ACTION_MESSAGE_SPEC = "INSERT OR REPLACE INTO " + TABLE_ACTION_MESSAGES +
            " (" +
            COL_ACTION_MESSAGE_ID + ", " +
            COL_ACTION_MESSAGE_CHANNEL_ID + ", " +
            COL_ACTION_MESSAGE_TARGET_MESSAGE_ID + ", " +
            COL_ACTION_MESSAGE_TARGET_CHANNEL_ID +
            ") " +
            "VALUES(?, ?, ?, ?)";

    private static ADNDatabase sInstance;

    private SQLiteDatabase mDatabase;
    private SQLiteStatement mInsertOrReplaceMessage;
    private SQLiteStatement mInsertOrReplaceHashtag;
    private SQLiteStatement mInsertOrReplaceGeolocation;
    private SQLiteStatement mInsertOrReplaceLocationInstance;
    private SQLiteStatement mInsertOrReplaceOEmbedInstance;
    private SQLiteStatement mInsertOrReplacePendingFile;
    private SQLiteStatement mInsertOrReplacePendingMessageDeletion;
    private SQLiteStatement mInsertOrReplacePendingOEmbed;
    private SQLiteStatement mInsertOrReplaceActionMessageSpec;
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

    private void insertOrReplacePendingOEmbed(String pendingFileId, String messageId, String channelId) {
        if(mInsertOrReplacePendingOEmbed == null) {
            mInsertOrReplacePendingOEmbed = mDatabase.compileStatement(INSERT_OR_REPLACE_PENDING_OEMBED);
        }
        mDatabase.beginTransaction();

        try {
            mInsertOrReplacePendingOEmbed.bindString(1, pendingFileId);
            mInsertOrReplacePendingOEmbed.bindString(2, messageId);
            mInsertOrReplacePendingOEmbed.bindString(3, channelId);
            mInsertOrReplacePendingOEmbed.execute();
            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
            mInsertOrReplacePendingOEmbed.clearBindings();
        }
    }

    public void insertOrReplaceMessage(MessagePlus messagePlus) {
        if(mInsertOrReplaceMessage == null) {
            mInsertOrReplaceMessage = mDatabase.compileStatement(INSERT_OR_REPLACE_MESSAGE);
        }
        mDatabase.beginTransaction();

        Date displayDate = messagePlus.getDisplayDate();
        Message message = messagePlus.getMessage();
        String text = message.getText();
        message.setText(null);

        try {
            mInsertOrReplaceMessage.bindString(1, message.getId());
            mInsertOrReplaceMessage.bindString(2, message.getChannelId());
            mInsertOrReplaceMessage.bindLong(3, displayDate.getTime());
            mInsertOrReplaceMessage.bindString(4, mGson.toJson(message));
            if(text != null) {
                mInsertOrReplaceMessage.bindString(5, text);
            } else {
                mInsertOrReplaceMessage.bindNull(5);
            }
            mInsertOrReplaceMessage.bindLong(6, messagePlus.isUnsent() ? 1 : 0);
            mInsertOrReplaceMessage.bindLong(7, messagePlus.getNumSendAttempts());
            mInsertOrReplaceMessage.execute();

            Set<String> pendingOEmbeds = messagePlus.getPendingOEmbeds();
            if(pendingOEmbeds != null) {
                for(String pendingOEmbed : pendingOEmbeds) {
                    insertOrReplacePendingOEmbed(pendingOEmbed, message.getId(), message.getChannelId());
                }
            }
            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
            mInsertOrReplaceMessage.clearBindings();
            message.setText(text);
        }
    }

    public Map<String, HashtagInstances> insertOrReplaceHashtagInstances(MessagePlus message) {
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

    public void insertOrReplaceGeolocation(Geolocation geolocation) {
        if(mInsertOrReplaceGeolocation == null) {
            mInsertOrReplaceGeolocation = mDatabase.compileStatement(INSERT_OR_REPLACE_GEOLOCATION);
        }
        mDatabase.beginTransaction();
        try {
            double latitude = getRoundedValue(geolocation.getLatitude(), 3);
            double longitude = getRoundedValue(geolocation.getLongitude(), 3);

            mInsertOrReplaceGeolocation.bindString(1, geolocation.getLocality());

            String subLocality = geolocation.getSubLocality();
            if(subLocality != null) {
                mInsertOrReplaceGeolocation.bindString(2, subLocality);
            } else {
                mInsertOrReplaceGeolocation.bindNull(2);
            }
            mInsertOrReplaceGeolocation.bindDouble(3, latitude);
            mInsertOrReplaceGeolocation.bindDouble(4, longitude);
            mInsertOrReplaceGeolocation.execute();
            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
            mInsertOrReplaceGeolocation.clearBindings();
        }
    }

    public void insertOrReplaceDisplayLocationInstance(MessagePlus messagePlus) {
        if(mInsertOrReplaceLocationInstance == null) {
            mInsertOrReplaceLocationInstance = mDatabase.compileStatement(INSERT_OR_REPLACE_LOCATION_INSTANCE);
        }
        DisplayLocation location = messagePlus.getDisplayLocation();
        if(location != null) {
            String name = location.getName();
            String shortName = location.getShortName();
            String messageId = messagePlus.getMessage().getId();
            String channelId = messagePlus.getMessage().getChannelId();
            String factualId = location.getFactualId();

            mDatabase.beginTransaction();
            try {
                mInsertOrReplaceLocationInstance.bindString(1, name);
                if(shortName != null) {
                    mInsertOrReplaceLocationInstance.bindString(2, shortName);
                } else {
                    mInsertOrReplaceLocationInstance.bindNull(2);
                }
                mInsertOrReplaceLocationInstance.bindString(3, messageId);
                mInsertOrReplaceLocationInstance.bindString(4, channelId);
                mInsertOrReplaceLocationInstance.bindDouble(5, location.getLatitude());
                mInsertOrReplaceLocationInstance.bindDouble(6, location.getLongitude());
                if(factualId != null) {
                    mInsertOrReplaceLocationInstance.bindString(7, factualId);
                } else {
                    mInsertOrReplaceLocationInstance.bindNull(7);
                }
                mInsertOrReplaceLocationInstance.bindLong(8, messagePlus.getDisplayDate().getTime());
                mInsertOrReplaceLocationInstance.execute();
                mDatabase.setTransactionSuccessful();
            } catch(Exception e) {
                Log.e(TAG, e.getMessage(), e);
            } finally {
                mDatabase.endTransaction();
                mInsertOrReplaceLocationInstance.clearBindings();
            }
        }
    }

    public void insertOrReplaceOEmbedInstances(MessagePlus messagePlus) {
        if(mInsertOrReplaceOEmbedInstance == null) {
            mInsertOrReplaceOEmbedInstance = mDatabase.compileStatement(INSERT_OR_REPLACE_OEMBED_INSTANCE);
        }
        if(messagePlus.hasSetOEmbedValues()) {
            mDatabase.beginTransaction();
            try {
                if(messagePlus.hasPhotoOEmbed()) {
                    insertOrReplaceOEmbedInstances(messagePlus, messagePlus.getPhotoOEmbeds());
                }
                if(messagePlus.hasHtml5VideoOEmbed()) {
                    insertOrReplaceOEmbedInstances(messagePlus, messagePlus.getHtml5VideoOEmbeds());
                }
                mDatabase.setTransactionSuccessful();
            } catch(Exception e) {
                Log.e(TAG, e.getMessage(), e);
            } finally {
                mDatabase.endTransaction();
            }
        }
    }

    //this assumes all oembeds are of the same type.
    private void insertOrReplaceOEmbedInstances(MessagePlus messagePlus, List<? extends MessagePlus.OEmbed> oEmbeds) {
        if(oEmbeds != null && oEmbeds.size() > 0) {
            mDatabase.beginTransaction();

            try {
                Message m = messagePlus.getMessage();
                mInsertOrReplaceOEmbedInstance.bindString(1, oEmbeds.get(0).getType());
                mInsertOrReplaceOEmbedInstance.bindString(2, m.getId());
                mInsertOrReplaceOEmbedInstance.bindString(3, m.getChannelId());
                mInsertOrReplaceOEmbedInstance.bindLong(4, oEmbeds.size());
                mInsertOrReplaceOEmbedInstance.bindLong(5, messagePlus.getDisplayDate().getTime());
                mInsertOrReplaceOEmbedInstance.execute();
                mDatabase.setTransactionSuccessful();
            } catch(Exception e) {
                Log.e(TAG, e.getMessage(), e);
            } finally {
                mDatabase.endTransaction();
                mInsertOrReplaceOEmbedInstance.clearBindings();
            }
        }
    }

    public void insertOrReplacePendingFile(PendingFile pendingFile) {
        insertOrReplacePendingFile(pendingFile.getId(), pendingFile.getUri().toString(), pendingFile.getType(), pendingFile.getName(), pendingFile.getMimeType(), pendingFile.getKind(), pendingFile.isPublic(), pendingFile.getNumSendAttempts());
    }

    public void insertOrReplacePendingFile(String id, String uri, String type, String name, String mimeType, String kind, boolean isPublic, int numSendAttempts) {
        if(mInsertOrReplacePendingFile == null) {
            mInsertOrReplacePendingFile = mDatabase.compileStatement(INSERT_OR_REPLACE_PENDING_FILE);
        }
        mDatabase.beginTransaction();

        try {
            mInsertOrReplacePendingFile.bindString(1, id);
            mInsertOrReplacePendingFile.bindString(2, uri);
            mInsertOrReplacePendingFile.bindString(3, type);
            mInsertOrReplacePendingFile.bindString(4, name);
            mInsertOrReplacePendingFile.bindString(5, mimeType);

            if(kind != null) {
                mInsertOrReplacePendingFile.bindString(6, kind);
            } else {
                mInsertOrReplacePendingFile.bindNull(6);
            }
            mInsertOrReplacePendingFile.bindLong(7, isPublic ? 1 : 0);
            mInsertOrReplacePendingFile.bindLong(8, numSendAttempts);
            mInsertOrReplacePendingFile.execute();
            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
            mInsertOrReplacePendingFile.clearBindings();
        }
    }

    public void insertOrReplacePendingDeletion(MessagePlus messagePlus, boolean deleteFiles) {
        if(mInsertOrReplacePendingMessageDeletion == null) {
            mInsertOrReplacePendingMessageDeletion = mDatabase.compileStatement(INSERT_OR_REPLACE_PENDING_MESSAGE_DELETION);
        }
        mDatabase.beginTransaction();

        try {
            Message message = messagePlus.getMessage();
            mInsertOrReplacePendingMessageDeletion.bindString(1, message.getId());
            mInsertOrReplacePendingMessageDeletion.bindString(2, message.getChannelId());
            mInsertOrReplacePendingMessageDeletion.bindLong(3, deleteFiles ? 1 : 0);
            mInsertOrReplacePendingMessageDeletion.execute();
            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
            mInsertOrReplacePendingMessageDeletion.clearBindings();
        }
    }

    public void insertOrReplaceActionMessageSpec(MessagePlus actionMessagePlus, String targetMessageId, String targetChannelId) {
        if(mInsertOrReplaceActionMessageSpec == null) {
            mInsertOrReplaceActionMessageSpec = mDatabase.compileStatement(INSERT_OR_REPLACE_ACTION_MESSAGE_SPEC);
        }
        mDatabase.beginTransaction();

        try {
            Message actionMessage = actionMessagePlus.getMessage();

            mInsertOrReplaceActionMessageSpec.bindString(1, actionMessage.getId());
            mInsertOrReplaceActionMessageSpec.bindString(2, actionMessage.getChannelId());
            mInsertOrReplaceActionMessageSpec.bindString(3, targetMessageId);
            mInsertOrReplaceActionMessageSpec.bindString(4, targetChannelId);
            mInsertOrReplaceActionMessageSpec.execute();
            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
            mInsertOrReplaceActionMessageSpec.clearBindings();
        }
    }

    public boolean hasActionMessageSpec(String actionChannelId, String targetMessageId) {
        Cursor cursor = null;
        try {
            String where = COL_ACTION_MESSAGE_CHANNEL_ID + " = ? AND " + COL_ACTION_MESSAGE_TARGET_MESSAGE_ID + " = ?";
            String[] args = new String[] { actionChannelId, targetMessageId };
            cursor = mDatabase.query(TABLE_ACTION_MESSAGES, null, where, args, null, null, null, String.valueOf(1));
            if(cursor.moveToNext()) {
                cursor.close();
                return true;
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return false;
    }

    public List<ActionMessageSpec> getActionMessageSpecsForTargetMessages(List<String> targetMessageIds) {
        return getActionMessageSpecsForTargetMessages(null, targetMessageIds);
    }

    public List<ActionMessageSpec> getActionMessageSpecsForTargetMessages(String actionChannelId, List<String> targetMessageIds) {
        ArrayList<ActionMessageSpec> actionMessageSpecs = new ArrayList<ActionMessageSpec>();
        Cursor cursor = null;
        try {
            String where = "";
            String[] args = null;
            int inStartIndex = 0;
            if(actionChannelId != null) {
                args = new String[targetMessageIds.size() + 1];
                args[0] = actionChannelId;
                where += COL_ACTION_MESSAGE_CHANNEL_ID + " = ? AND ";
                inStartIndex = 1;
            } else {
                args = new String[targetMessageIds.size()];
            }

            where += COL_ACTION_MESSAGE_TARGET_MESSAGE_ID + " IN (";

            int index = inStartIndex;
            Iterator<String> iterator = targetMessageIds.iterator();
            while(iterator.hasNext()) {
                args[index] = iterator.next();
                if(index > inStartIndex) {
                    where += ", ?";
                } else {
                    where += " ?";
                }
                index++;
            }
            where += ")";
            cursor = mDatabase.query(TABLE_ACTION_MESSAGES, null, where, args, null, null, null, null);

            while(cursor.moveToNext()) {
                String aMessageId = cursor.getString(0);
                String aChannelId = cursor.getString(1);
                String tMessageId = cursor.getString(2);
                String tChannelId = cursor.getString(3);

                ActionMessageSpec actionMessageSpec = new ActionMessageSpec(aMessageId, aChannelId, tMessageId, tChannelId);
                actionMessageSpecs.add(actionMessageSpec);
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return actionMessageSpecs;
    }

    public PendingFile getPendingFile(String id) {
        Cursor cursor = null;
        PendingFile file = null;
        try {
            String where = COL_PENDING_FILE_ID + " = ?";
            String args[] = new String[] { id };
            cursor = mDatabase.query(TABLE_PENDING_FILES, null, where, args, null, null, null, null);

            if(cursor.moveToNext()) {
                String uri = cursor.getString(1);
                String type = cursor.getString(2);
                String name = cursor.getString(3);
                String mimeType = cursor.getString(4);
                String kind = cursor.getString(5);
                boolean isPublic = cursor.getInt(6) == 1;
                int sendAttempts = cursor.getInt(7);

                file = new PendingFile(id, Uri.parse(uri), type, name, mimeType, kind, isPublic, sendAttempts);
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return file;
    }

    public Set<String> getPendingOEmbeds(String messageId, String channelId) {
        HashSet<String> pendingOEmbeds = new HashSet<String>();
        Cursor cursor = null;
        try {
            String where = COL_PENDING_OEMBED_MESSAGE_ID + " = ? AND " + COL_PENDING_OEMBED_CHANNEL_ID + " = ?";
            String args[] = new String[] { messageId, channelId };
            cursor = mDatabase.query(TABLE_PENDING_OEMBEDS, new String[] { COL_PENDING_OEMBED_PENDING_FILE_ID }, where, args, null, null, null, null);

            while(cursor.moveToNext()) {
                String pendingFileId = cursor.getString(0);
                pendingOEmbeds.add(pendingFileId);
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return pendingOEmbeds;
    }

    /**
     * Get an OEmbedInstances object representing the complete set of messages with an OEmbed
     * Annotation of the specified type. The type String comes from the "type" value of the actual
     * OEmbed Annotation, e.g. photo, html5video
     *
     * @param channelId the Channel id
     * @param type The OEmbed type
     *
     * @return OEmbedInstances
     */
    public OEmbedInstances getOEmbedInstances(String channelId, String type) {
        Cursor cursor = null;
        OEmbedInstances instances = new OEmbedInstances(type);
        try {
            String where = COL_OEMBED_INSTANCE_CHANNEL_ID + " = ? AND " + COL_OEMBED_INSTANCE_TYPE + " = ?";
            String[] args = new String[] { channelId, type };
            cursor = mDatabase.query(TABLE_OEMBED_INSTANCES, new String[] { COL_OEMBED_INSTANCE_MESSAGE_ID }, where, args, null, null, null, null);

            if(cursor.moveToNext()) {
                do {
                    String messageId = cursor.getString(0);
                    instances.addInstance(messageId);
                } while(cursor.moveToNext());
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return instances;
    }

    /**
     * Get all DisplayLocationInstances in the specified channel.
     *
     * @param channelId the Channel id
     * @return a List of DisplaylocationInstances in descending order, from most to least recent
     */
    public List<DisplayLocationInstances> getDisplayLocationInstances(String channelId) {
        LinkedHashMap<String, DisplayLocationInstances> allInstances = new LinkedHashMap<String, DisplayLocationInstances>();

        Cursor cursor = null;
        try {
            String where = COL_LOCATION_INSTANCE_CHANNEL_ID + " = ?";
            String args[] = new String[] { channelId };
            String orderBy = COL_LOCATION_INSTANCE_DATE + " DESC";

            String[] cols = new String[] { COL_LOCATION_INSTANCE_NAME, COL_LOCATION_INSTANCE_SHORT_NAME, COL_LOCATION_INSTANCE_MESSAGE_ID, COL_LOCATION_INSTANCE_LATITUDE, COL_LOCATION_INSTANCE_LONGITUDE };
            cursor = mDatabase.query(TABLE_LOCATION_INSTANCES, cols, where, args, null, null, orderBy, null);
            if(cursor.moveToNext()) {
                do {
                    String name = cursor.getString(0);
                    String shortName = cursor.getString(1);
                    String messageId = cursor.getString(2);
                    Double latitude = cursor.getDouble(3);
                    Double longitude = cursor.getDouble(4);

                    double roundedLat = getRoundedValue(latitude, 1);
                    double roundedLong = getRoundedValue(longitude, 1);

                    String key = String.format("%s %s %s", name, String.valueOf(roundedLat), String.valueOf(roundedLong));
                    DisplayLocationInstances displayLocationInstances = allInstances.get(key);
                    if(displayLocationInstances == null) {
                        DisplayLocation loc = new DisplayLocation(name, latitude, longitude);
                        loc.setShortName(shortName);
                        displayLocationInstances = new DisplayLocationInstances(loc);
                        allInstances.put(key, displayLocationInstances);
                    }
                    displayLocationInstances.addInstance(messageId);
                } while(cursor.moveToNext());
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return Arrays.asList(allInstances.values().toArray(new DisplayLocationInstances[0]));
    }

    /**
     * Get a DisplayLocationInstances object representing the complete set of messages with which
     * the specified DisplayLocation is associated. This defaults the lookup to a precision of
     * LocationPrecision.ONE_HUNDRED_METERS (actually ~111 m)
     *
     * Display locations are unique by name + latitude + longitude, where latitude and longitude are
     *
     *
     * @param channelId The id of the channel
     * @param location the DisplayLocation
     *
     * @return DisplayLocationInstances
     *
     * @see com.alwaysallthetime.adnlibutils.db.ADNDatabase#getDisplayLocationInstances(String, com.alwaysallthetime.adnlibutils.model.DisplayLocation, com.alwaysallthetime.adnlibutils.db.ADNDatabase.LocationPrecision)
     */
    public DisplayLocationInstances getDisplayLocationInstances(String channelId, DisplayLocation location) {
        return getDisplayLocationInstances(channelId, location, LocationPrecision.ONE_HUNDRED_METERS);
    }

    /**
     * Get a DisplayLocationInstances object representing the complete set of messages with which
     * the specified DisplayLocation is associated. Because DisplayLocations might have names that
     * represent larger geographic areas, this method accepts a LocationPrecision that acts as
     * a filter. For example, if a location was simply, "Mission District, San Francisco," you might want
     * to provide a wider LocationPrecision so that a location that has the same name, but slightly different
     * coordinates are returned.
     *
     * Display locations are unique by name + latitude + longitude, where latitude and longitude are
     * always rounded to three decimal places. So, by providing a less precise LocationPrecision, you
     * can lookup by locations that match, e.g. 2 or 1 decimal places.
     *
     * @param channelId The id of the channel
     * @param location the DisplayLocation
     * @param precision The precision to use when obtaining location instances.
     *
     * @return DisplayLocationInstances
     */
    public DisplayLocationInstances getDisplayLocationInstances(String channelId, DisplayLocation location, LocationPrecision precision) {
        Cursor cursor = null;
        DisplayLocationInstances instances = new DisplayLocationInstances(location);
        try {
            String where = COL_LOCATION_INSTANCE_CHANNEL_ID + " = ? AND " + COL_LOCATION_INSTANCE_NAME + " = ? AND " +
                           COL_LOCATION_INSTANCE_LATITUDE + " LIKE ? AND " + COL_LOCATION_INSTANCE_LONGITUDE + " LIKE ?";

            String latArg = null;
            String longArg = null;
            int precisionDigits = 3;
            if(precision == LocationPrecision.ONE_THOUSAND_METERS) {
                precisionDigits = 2;
            } else if(precision == LocationPrecision.TEN_THOUSAND_METERS) {
                precisionDigits = 1;
            }

            latArg = String.format("%s%%", String.valueOf(getRoundedValue(location.getLatitude(), precisionDigits)));
            longArg = String.format("%s%%", String.valueOf(getRoundedValue(location.getLongitude(), precisionDigits)));

            String orderBy = COL_LOCATION_INSTANCE_DATE + " DESC";
            String[] args = new String[] { channelId, location.getName(), latArg, longArg };
            String[] cols = new String[] { COL_LOCATION_INSTANCE_MESSAGE_ID };
            cursor = mDatabase.query(TABLE_LOCATION_INSTANCES, cols, where, args, null, null, orderBy, null);
            if(cursor.moveToNext()) {
                do {
                    String messageId = cursor.getString(0);
                    instances.addInstance(messageId);
                } while(cursor.moveToNext());
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return instances;
    }

    /**
     * Look for a Gelocation by latitude and longitude. Coordinates are rounded to three decimal
     * places (0.001 = 111 meters).
     *
     * @param latitude
     * @param longitude
     * @return a Geolocation if one exists, null otherwise.
     */
    public Geolocation getGeolocation(double latitude, double longitude) {
        Cursor cursor = null;
        try {
            String[] args = new String[] { String.valueOf(getRoundedValue(latitude, 3)), String.valueOf(getRoundedValue(longitude, 3))};
            String where = COL_GEOLOCATION_LATITUDE + " = ? AND " + COL_GEOLOCATION_LONGITUDE + " = ?";
            cursor = mDatabase.query(TABLE_GEOLOCATIONS, new String[] { COL_GEOLOCATION_LOCALITY, COL_GEOLOCATION_SUBLOCALITY }, where, args, null, null, null, null);

            if(cursor.moveToNext()) {
                String locality = cursor.getString(0);
                String subLocality = cursor.getString(1);
                cursor.close();
                return new Geolocation(locality, subLocality, latitude, longitude);
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return null;
    }


    /**
     * Get all Hashtags for a channel sorted in descending order – from most recently used to
     * least recently used.
     *
     * @return a Map whose keys are hashtag names, mapped to HashtagInstances
     */
    public LinkedHashMap<String, HashtagInstances> getHashtagInstances(String channelId) {
        return getHashtagInstances(channelId, null, null);
    }

    /**
     * Get all Hashtags for a channel whose associated messages were created since a specified date.
     * Results are sorted in descending order – from most recently used to least recently used.
     *
     * @param channelId The id of the channel
     * @param sinceDate The earliest date for which hashtags should be returned.
     * @return a Map whose keys are hashtag names, mapped to HashtagInstances
     */
    public LinkedHashMap<String, HashtagInstances> getHashtagInstances(String channelId, Date sinceDate) {
        return getHashtagInstances(channelId, null, sinceDate);
    }

    /**
     * Get all Hashtags for a channel whose associated messages were created within the provided
     * date window, i.e. (beforeDate, sinceDate]. If no dates are provided, then all Hashtags are
     * returned. Results are sorted in descending order – from most recently used to least recently
     * used.
     *
     * @param channelId The id of the channel
     * @param beforeDate The date before which all hashtags' associated messages were created. Can be null.
     * @param sinceDate The earliest date for which hashtags should be returned. Can be null.
     *
     * @return a Map whose keys are hashtag names, mapped to HashtagInstances
     *
     * @see com.alwaysallthetime.adnlibutils.db.ADNDatabase#getHashtagInstances(String, java.util.Date)
     */
    public LinkedHashMap<String, HashtagInstances> getHashtagInstances(String channelId, Date beforeDate, Date sinceDate) {
        LinkedHashMap<String, HashtagInstances> instances = new LinkedHashMap<String, HashtagInstances>();
        Cursor cursor = null;
        try {
            String where = COL_HASHTAG_INSTANCE_CHANNEL_ID + " =?";
            ArrayList<String> args = new ArrayList<String>(3);
            args.add(channelId);

            if(sinceDate != null) {
                where += " AND " + COL_HASHTAG_INSTANCE_DATE + " >= ?";
                args.add(String.valueOf(sinceDate.getTime()));
            }
            if(beforeDate != null) {
                where += " AND " + COL_HASHTAG_INSTANCE_DATE + " < ?";
                args.add(String.valueOf(beforeDate.getTime()));
            }

            String orderBy = COL_HASHTAG_INSTANCE_DATE + " DESC";
            cursor = mDatabase.query(TABLE_HASHTAG_INSTANCES, null, where, args.toArray(new String[0]), null, null, orderBy, null);

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
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return instances;
    }

    /**
     * Get a HashtagInstances object representing all Messages in which the specified
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
            String where = COL_HASHTAG_INSTANCE_CHANNEL_ID + " =? AND " + COL_HASHTAG_INSTANCE_NAME + " = ?";
            String[] args = new String[] { channelId, hashtagName };
            cursor = mDatabase.query(TABLE_HASHTAG_INSTANCES, new String[] { COL_HASHTAG_INSTANCE_MESSAGE_ID }, where, args, null, null, null, null);

            if(cursor.moveToNext()) {
                do {
                    String messageId = cursor.getString(0);
                    instances.addInstance(messageId);
                } while(cursor.moveToNext());
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return instances;
    }

    public OrderedMessageBatch searchForMessages(String channelId, String query) {
        String where = COL_MESSAGE_CHANNEL_ID + " = ? AND " + COL_MESSAGE_TEXT + " MATCH ?";
        String[] args = new String[] { channelId, query };
        return getMessages(where, args, null, null);
    }

    public MessagePlus getMessage(String channelId, String messageId) {
        HashSet<String> ids = new HashSet<String>(1);
        ids.add(messageId);
        OrderedMessageBatch messages = getMessages(channelId, ids);
        LinkedHashMap<String, MessagePlus> orderedMessages = messages.getMessages();
        if(orderedMessages.size() == 1) {
            return orderedMessages.values().iterator().next();
        }
        return null;
    }

    /**
     * Get Messages in a Channel
     *
     * @param channelId The id of the Channel.
     * @param messageIds The ids of the Messages to get.
     * @return OrderedMessageBatch
     */
    public OrderedMessageBatch getMessages(String channelId, Collection<String> messageIds) {
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
        return getMessages(where, args, orderBy, null);
    }

    public OrderedMessageBatch getMessages(String channelId, int limit) {
        return getMessages(channelId, null, limit);
    }

    public OrderedMessageBatch getMessages(String channelId, Date beforeDate, int limit) {
        String where = COL_MESSAGE_CHANNEL_ID + " =?";
        String[] args = null;

        if(beforeDate != null) {
            where += " AND " + COL_MESSAGE_DATE + " < ?";
            args = new String[] { channelId,  String.valueOf(beforeDate.getTime()) };
        } else {
            args = new String[] { channelId };
        }
        String orderBy = COL_MESSAGE_DATE + " DESC";
        return getMessages(where, args, orderBy, String.valueOf(limit));
    }

    private OrderedMessageBatch getMessages(String where, String[] args, String orderBy, String limit) {
        LinkedHashMap<String, MessagePlus> messages = new LinkedHashMap<String, MessagePlus>();
        ArrayList<MessagePlus> unsentMessages = new ArrayList<MessagePlus>();
        String maxId = null;
        String minId = null;
        Cursor cursor = null;
        try {
            cursor = mDatabase.query(TABLE_MESSAGES, null, where, args, null, null, orderBy, limit);

            Message message = null;
            if(cursor.moveToNext()) {
                do {
                    String messageId = cursor.getString(0);
                    long date = cursor.getLong(2);
                    String messageJson = cursor.getString(3);
                    String messageText = cursor.getString(4);
                    boolean isUnsent = cursor.getInt(5) == 1;
                    int numSendAttempts = cursor.getInt(6);
                    message = mGson.fromJson(messageJson, Message.class);
                    message.setText(messageText);

                    MessagePlus messagePlus = new MessagePlus(message);
                    messagePlus.setDisplayDate(new Date(date));
                    messagePlus.setIsUnsent(isUnsent);
                    messagePlus.setNumSendAttempts(numSendAttempts);
                    messages.put(messageId, messagePlus);

                    if(maxId == null) {
                        maxId = messageId;
                    }
                    if(isUnsent) {
                        unsentMessages.add(messagePlus);
                    }
                } while(cursor.moveToNext());

                if(message != null) {
                    minId = message.getId();
                }
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        for(MessagePlus messagePlus : unsentMessages) {
            Message message = messagePlus.getMessage();
            Set<String> pendingOEmbeds = getPendingOEmbeds(message.getId(), message.getChannelId());
            messagePlus.setPendingOEmbeds(pendingOEmbeds);
        }
        return new OrderedMessageBatch(messages, new MinMaxPair(minId, maxId));
    }

    /**
     * Get all messages marked as unsent in a channel.
     * 
     * Unlike other message getters, this one returns the messages in ascending order
     * (i.e. chronological order) - because this is the order in which they should be sent
     * to the server.
     *
     * @param channelId
     * @return a LinkedHashMap with message ids as keys, mapped to MessagePlus objects.
     */
    public LinkedHashMap<String, MessagePlus> getUnsentMessages(String channelId) {
        LinkedHashMap<String, MessagePlus> unsentMessages = new LinkedHashMap<String, MessagePlus>();

        Cursor cursor = null;
        try {
            String where = COL_MESSAGE_CHANNEL_ID + " = ? AND " + COL_MESSAGE_UNSENT + " = ?";
            String[] args = new String[] { channelId, String.valueOf(1) };
            String[] cols = new String[] { COL_MESSAGE_ID, COL_MESSAGE_DATE, COL_MESSAGE_JSON, COL_MESSAGE_TEXT, COL_MESSAGE_SEND_ATTEMPTS };
            String orderBy = COL_MESSAGE_DATE + " ASC";

            cursor = mDatabase.query(TABLE_MESSAGES, cols, where, args, null, null, orderBy, null);
            while(cursor.moveToNext()) {
                String messageId = cursor.getString(0);
                long date = cursor.getLong(1);
                String messageJson = cursor.getString(2);
                String messageText = cursor.getString(3);
                int sendAttempts = cursor.getInt(4);

                Message message = mGson.fromJson(messageJson, Message.class);
                message.setText(messageText);

                MessagePlus messagePlus = new MessagePlus(message);
                messagePlus.setDisplayDate(new Date(date));
                messagePlus.setIsUnsent(true);
                messagePlus.setNumSendAttempts(sendAttempts);
                unsentMessages.put(messageId, messagePlus);
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        for(MessagePlus messagePlus : unsentMessages.values()) {
            Message message = messagePlus.getMessage();
            Set<String> pendingOEmbeds = getPendingOEmbeds(message.getId(), message.getChannelId());
            messagePlus.setPendingOEmbeds(pendingOEmbeds);
        }
        return unsentMessages;
    }

    public HashMap<String, PendingMessageDeletion> getPendingMessageDeletions(String channelId) {
        HashMap<String, PendingMessageDeletion> deletions = new HashMap<String, PendingMessageDeletion>();

        Cursor cursor = null;
        try {
            String where = COL_PENDING_MESSAGE_DELETION_CHANNEL_ID + " = ?";
            String[] args = new String[] { channelId };
            String[] cols = new String[] { COL_PENDING_MESSAGE_DELETION_MESSAGE_ID, COL_PENDING_MESSAGE_DELETION_DELETE_ASSOCIATED_FILES };
            cursor = mDatabase.query(TABLE_PENDING_MESSAGE_DELETIONS, cols, where, args, null, null, null, null);

            while(cursor.moveToNext()) {
                String messageId = cursor.getString(0);
                boolean deleteAssociatedFiles = cursor.getInt(1) == 1;
                PendingMessageDeletion deletion = new PendingMessageDeletion(messageId, channelId, deleteAssociatedFiles);
                deletions.put(messageId, deletion);
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return deletions;
    }

    public void deleteMessage(MessagePlus messagePlus) {
        mDatabase.beginTransaction();

        try {
            Message message = messagePlus.getMessage();
            mDatabase.delete(TABLE_MESSAGES, COL_MESSAGE_ID + " = '" + message.getId() + "'", null);

            //this might be null in the case of unsent messages.
            Entities entities = message.getEntities();
            if(entities != null) {
                ArrayList<Entities.Hashtag> hashtags = entities.getHashtags();
                for(Entities.Hashtag h : hashtags) {
                    String where = COL_HASHTAG_INSTANCE_NAME + " = '" + h.getName() + "' AND " + COL_HASHTAG_INSTANCE_MESSAGE_ID + " = '" + message.getId() + "'";
                    mDatabase.delete(TABLE_HASHTAG_INSTANCES, where, null);
                }
            }

            DisplayLocation l = messagePlus.getDisplayLocation();
            if(l != null) {
                String where = COL_LOCATION_INSTANCE_NAME + " = " + DatabaseUtils.sqlEscapeString(l.getName()) + " AND " + COL_LOCATION_INSTANCE_MESSAGE_ID + " = " + message.getId() +
                                                                                   " AND " + COL_LOCATION_INSTANCE_LATITUDE + " = " + l.getLatitude() +
                                                                                    " AND " + COL_LOCATION_INSTANCE_LONGITUDE + " = " + l.getLongitude();
                mDatabase.delete(TABLE_LOCATION_INSTANCES, where, null);
            }

            if(messagePlus.hasPhotoOEmbed()) {
                deleteOEmbedInstances(messagePlus.getPhotoOEmbeds().get(0).getType(), message.getId());
            }
            if(messagePlus.hasHtml5VideoOEmbed()) {
                deleteOEmbedInstances(messagePlus.getHtml5VideoOEmbeds().get(0).getType(), message.getId());
            }
            if(messagePlus.hasPendingOEmbeds()) {
                Set<String> pendingOEmbeds = messagePlus.getPendingOEmbeds();
                for(String pendingFileId : pendingOEmbeds) {
                    deletePendingOEmbed(pendingFileId, message.getId(), message.getChannelId());

                    //TODO: can multiple message plus objects use the same pending file Id?
                    //if so, we shouldn't do this here - must make sure no other MPs need it.
                    deletePendingFile(pendingFileId);
                }
            }

            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
        }
    }

    public void deletePendingMessageDeletion(String messageId) {
        mDatabase.delete(TABLE_PENDING_MESSAGE_DELETIONS, COL_PENDING_MESSAGE_DELETION_MESSAGE_ID + " = '" + messageId + "'", null);
    }

    private void deleteOEmbedInstances(String type, String messageId) {
        String where = COL_OEMBED_INSTANCE_TYPE + " = " + "'" + type + "' AND " + COL_OEMBED_INSTANCE_MESSAGE_ID + " = " + "'" + messageId + "'";
        mDatabase.delete(TABLE_OEMBED_INSTANCES, where, null);
    }

    public void deleteMessages(String channelId) {
        mDatabase.beginTransaction();

        try {
            mDatabase.delete(TABLE_MESSAGES, COL_MESSAGE_CHANNEL_ID + " = '" + channelId + "'", null);
            mDatabase.delete(TABLE_HASHTAG_INSTANCES, COL_HASHTAG_INSTANCE_CHANNEL_ID + " = '" + channelId + "'", null);
            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
        }
    }

    public void deleteActionMessageSpec(String actionChannelId, String targetMessageId) {
        String where = COL_ACTION_MESSAGE_CHANNEL_ID + " = " + "'" + actionChannelId + "' AND " + COL_ACTION_MESSAGE_TARGET_MESSAGE_ID + " = " + "'" + targetMessageId + "'";
        mDatabase.delete(TABLE_ACTION_MESSAGES, where, null);
    }

    public void deleteActionMessageSpec(String actionMessageId) {
        String where = COL_ACTION_MESSAGE_ID + " = '" + actionMessageId + "'";
        mDatabase.delete(TABLE_ACTION_MESSAGES, where, null);
    }

    public void deletePendingFile(String pendingFileId) {
        mDatabase.beginTransaction();

        try {
            String where = COL_PENDING_FILE_ID + " = '" + pendingFileId + "'";
            mDatabase.delete(TABLE_PENDING_FILES, where, null);
            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
        }
    }

    public void deletePendingOEmbed(String pendingFileId, String messageId, String channelId) {
        mDatabase.beginTransaction();

        try {
            String where = COL_PENDING_OEMBED_PENDING_FILE_ID + " = '" + pendingFileId + "' AND " +
                           COL_PENDING_OEMBED_MESSAGE_ID + " = '" + messageId + "' AND " +
                           COL_PENDING_OEMBED_CHANNEL_ID + " = '" + channelId + "'";
            mDatabase.delete(TABLE_PENDING_OEMBEDS, where, null);
            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
        }
    }

    private double getRoundedValue(double value, int numDecimals) {
        BigDecimal bigValue = new BigDecimal(value);
        return bigValue.setScale(numDecimals, BigDecimal.ROUND_DOWN).doubleValue();
    }
}
