package com.alwaysallthetime.messagebeast.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.alwaysallthetime.adnlib.data.Annotation;
import com.alwaysallthetime.adnlib.data.Entities;
import com.alwaysallthetime.adnlib.data.Message;
import com.alwaysallthetime.adnlib.data.Place;
import com.alwaysallthetime.adnlib.gson.AppDotNetGson;
import com.alwaysallthetime.messagebeast.manager.MinMaxPair;
import com.alwaysallthetime.messagebeast.manager.ReverseChronologicalComparator;
import com.alwaysallthetime.messagebeast.model.CustomPlace;
import com.alwaysallthetime.messagebeast.model.DisplayLocation;
import com.alwaysallthetime.messagebeast.model.Geolocation;
import com.alwaysallthetime.messagebeast.model.MessagePlus;
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
import java.util.TreeMap;

public class ADNDatabase {

    private static final String TAG = "ADNLibUtils_ADNDatabase";
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

    public static final String TABLE_MESSAGES_SEARCH = "messages_search";

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

    public static final String TABLE_LOCATION_INSTANCES_SEARCH = "locations_search";

    public static final String TABLE_PLACES = "places";
    public static final String COL_PLACE_ID = "place_id";
    public static final String COL_PLACE_NAME = "place_name";
    public static final String COL_PLACE_ROUNDED_LATITUDE = "place_rounded_latitude";
    public static final String COL_PLACE_ROUNDED_LONGITUDE = "place_rounded_longitude";
    public static final String COL_PLACE_IS_CUSTOM = "place_is_custom";
    public static final String COL_PLACE_JSON = "place_json";

    public static final String TABLE_ANNOTATION_INSTANCES = "annotation_instances";
    public static final String COL_ANNOTATION_INSTANCE_TYPE = "annotation_instance_type";
    public static final String COL_ANNOTATION_INSTANCE_MESSAGE_ID = "annotation_instance_message_id";
    public static final String COL_ANNOTATION_INSTANCE_CHANNEL_ID = "annotation_instance_channel_id";
    public static final String COL_ANNOTATION_INSTANCE_COUNT = "annotation_instance_count";
    public static final String COL_ANNOTATION_INSTANCE_DATE = "annotation_instance_date";

    public static final String TABLE_PENDING_FILES = "pending_files";
    public static final String COL_PENDING_FILE_ID = "pending_file_id";
    public static final String COL_PENDING_FILE_URI = "pending_file_uri";
    public static final String COL_PENDING_FILE_TYPE = "pending_file_type";
    public static final String COL_PENDING_FILE_NAME = "pending_file_name";
    public static final String COL_PENDING_FILE_MIMETYPE = "pending_file_mimetype";
    public static final String COL_PENDING_FILE_KIND = "pending_file_kind";
    public static final String COL_PENDING_FILE_PUBLIC = "pending_file_public";
    public static final String COL_PENDING_FILE_SEND_ATTEMPTS = "pending_file_send_attempts";

    public static final String TABLE_PENDING_FILE_ATTACHMENTS = "pending_file_attachments";
    public static final String COL_PENDING_FILE_ATTACHMENT_PENDING_FILE_ID = "pending_file_attachment_file_id";
    public static final String COL_PENDING_FILE_ATTACHMENT_MESSAGE_ID = "pending_file_attachment_message_id";
    public static final String COL_PENDING_FILE_ATTACHMENT_CHANNEL_ID = "pending_file_attachment_channel_id";
    public static final String COL_PENDING_FILE_ATTACHMENT_IS_OEMBED = "pending_file_attachment_is_oembed";

    public static final String TABLE_PENDING_MESSAGE_DELETIONS = "pending_message_deletions";
    public static final String COL_PENDING_MESSAGE_DELETION_MESSAGE_ID = "pending_message_deletion_message_id";
    public static final String COL_PENDING_MESSAGE_DELETION_CHANNEL_ID = "pending_message_deletion_channel_id";
    public static final String COL_PENDING_MESSAGE_DELETION_DELETE_ASSOCIATED_FILES = "pending_message_deletion_delete_files";

    public static final String TABLE_PENDING_FILE_DELETIONS = "pending_file_deletions";
    public static final String COL_PENDING_FILE_DELETION_FILE_ID = "pending_file_deletion_file_id";

    public static final String TABLE_ACTION_MESSAGES = "action_messages";
    public static final String COL_ACTION_MESSAGE_ID = "action_message_id";
    public static final String COL_ACTION_MESSAGE_CHANNEL_ID = "action_channel_id";
    public static final String COL_ACTION_MESSAGE_TARGET_MESSAGE_ID = "action_target_message_id";
    public static final String COL_ACTION_MESSAGE_TARGET_CHANNEL_ID = "action_target_channel_id";
    public static final String COL_ACTION_MESSAGE_TARGET_MESSAGE_DISPLAY_DATE = "action_target_message_display_date";

    /**
     * Precision values to be used when retrieving location instances.
     *
     * These values are approximate.
     */
    public enum LocationPrecision {
        ONE_HUNDRED_METERS, //actually 111 m
        ONE_THOUSAND_METERS, //actually 1.11 km
        TEN_THOUSAND_METERS; //actually 11.1 km

        public static int getNumPrecisionDigits(LocationPrecision precision) {
            int precisionDigits = 3;
            if(precision == LocationPrecision.ONE_THOUSAND_METERS) {
                precisionDigits = 2;
            } else if(precision == LocationPrecision.TEN_THOUSAND_METERS) {
                precisionDigits = 1;
            }
            return precisionDigits;
        }
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

    private static final String INSERT_MESSAGE_SEARCH_TEXT = "INSERT INTO " + TABLE_MESSAGES_SEARCH +
            " (docid, " + COL_MESSAGE_CHANNEL_ID + ", " + COL_MESSAGE_TEXT + ") " +
            "VALUES (?, ?, ?)";

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
            COL_LOCATION_INSTANCE_MESSAGE_ID + ", " +
            COL_LOCATION_INSTANCE_NAME + ", " +
            COL_LOCATION_INSTANCE_SHORT_NAME + ", " +
            COL_LOCATION_INSTANCE_CHANNEL_ID + ", " +
            COL_LOCATION_INSTANCE_LATITUDE + ", " +
            COL_LOCATION_INSTANCE_LONGITUDE + ", " +
            COL_LOCATION_INSTANCE_FACTUAL_ID + ", " +
            COL_LOCATION_INSTANCE_DATE +
            ") " +
            "VALUES(?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_OR_REPLACE_PLACE = "INSERT OR REPLACE INTO " + TABLE_PLACES +
            " (" +
            COL_PLACE_ID + ", " +
            COL_PLACE_NAME + ", " +
            COL_PLACE_ROUNDED_LATITUDE + ", " +
            COL_PLACE_ROUNDED_LONGITUDE + ", " +
            COL_PLACE_IS_CUSTOM + ", " +
            COL_PLACE_JSON +
            ") " +
            "VALUES(?, ?, ?, ?, ?, ?)";

    private static final String INSERT_LOCATION_INSTANCES_SEARCH_TEXT = "INSERT INTO " + TABLE_LOCATION_INSTANCES_SEARCH +
            " (docid, " + COL_LOCATION_INSTANCE_CHANNEL_ID + ", " + COL_LOCATION_INSTANCE_NAME + ") " +
            "VALUES (?, ?, ?)";

    private static final String INSERT_OR_REPLACE_ANNOTATION_INSTANCE = "INSERT OR REPLACE INTO " + TABLE_ANNOTATION_INSTANCES +
            " (" +
            COL_ANNOTATION_INSTANCE_TYPE + ", " +
            COL_ANNOTATION_INSTANCE_MESSAGE_ID + ", " +
            COL_ANNOTATION_INSTANCE_CHANNEL_ID + ", " +
            COL_ANNOTATION_INSTANCE_COUNT + ", " +
            COL_ANNOTATION_INSTANCE_DATE +
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

    private static final String INSERT_OR_REPLACE_PENDING_FILE_DELETION = "INSERT OR REPLACE INTO " + TABLE_PENDING_FILE_DELETIONS +
            " (" +
            COL_PENDING_FILE_DELETION_FILE_ID +
            ") " +
            "VALUES(?)";

    private static final String INSERT_OR_REPLACE_PENDING_FILE_ATTACHMENT = "INSERT OR REPLACE INTO " + TABLE_PENDING_FILE_ATTACHMENTS +
            " (" +
            COL_PENDING_FILE_ATTACHMENT_PENDING_FILE_ID + ", " +
            COL_PENDING_FILE_ATTACHMENT_MESSAGE_ID + ", " +
            COL_PENDING_FILE_ATTACHMENT_CHANNEL_ID + ", " +
            COL_PENDING_FILE_ATTACHMENT_IS_OEMBED +
            ") " +
            "VALUES(?, ?, ?, ?)";

    private static final String INSERT_OR_REPLACE_ACTION_MESSAGE_SPEC = "INSERT OR REPLACE INTO " + TABLE_ACTION_MESSAGES +
            " (" +
            COL_ACTION_MESSAGE_ID + ", " +
            COL_ACTION_MESSAGE_CHANNEL_ID + ", " +
            COL_ACTION_MESSAGE_TARGET_MESSAGE_ID + ", " +
            COL_ACTION_MESSAGE_TARGET_CHANNEL_ID + ", " +
            COL_ACTION_MESSAGE_TARGET_MESSAGE_DISPLAY_DATE +
            ") " +
            "VALUES(?, ?, ?, ?, ?)";

    private static ADNDatabase sInstance;

    private SQLiteDatabase mDatabase;
    private SQLiteStatement mInsertOrReplaceMessage;
    private SQLiteStatement mInsertMessageSearchText;
    private SQLiteStatement mInsertOrReplaceHashtag;
    private SQLiteStatement mInsertOrReplaceGeolocation;
    private SQLiteStatement mInsertOrReplacePlace;
    private SQLiteStatement mInsertOrReplaceLocationInstance;
    private SQLiteStatement mInsertLocationInstanceSearchText;
    private SQLiteStatement mInsertOrReplaceAnnotationInstance;
    private SQLiteStatement mInsertOrReplacePendingFile;
    private SQLiteStatement mInsertOrReplacePendingMessageDeletion;
    private SQLiteStatement mInsertOrReplacePendingFileDeletion;
    private SQLiteStatement mInsertOrReplacePendingFileAttachment;
    private SQLiteStatement mInsertOrReplaceActionMessageSpec;
    private Gson mGson;

    public static synchronized ADNDatabase getInstance(Context context) {
        if(sInstance == null) {
            sInstance = new ADNDatabase(context);
        }
        return sInstance;
    }

    //fts4 available in 11+
    //http://stackoverflow.com/questions/2421189/version-of-sqlite-used-in-android/4377116#4377116
    public static boolean isFullTextSearchAvailable() {
        return Build.VERSION.SDK_INT >= 11;
    }

    //http://www.sqlite.org/optoverview.html#minmax
    public Integer getMaxMessageId() {
        Cursor cursor = mDatabase.rawQuery("SELECT MAX(" + COL_MESSAGE_ID + ") FROM " + TABLE_MESSAGES, null);
        if(cursor.moveToFirst()) {
            return cursor.getInt(0);
        }
        return null;
    }

    private ADNDatabase(Context context) {
        ADNDatabaseOpenHelper openHelper = new ADNDatabaseOpenHelper(context, DB_NAME, null, DB_VERSION);
        mDatabase = openHelper.getWritableDatabase();
        mGson = AppDotNetGson.getPersistenceInstance();
    }

    private void insertOrReplacePendingFileAttachment(String pendingFileId, String messageId, String channelId, boolean isOEmbed) {
        if(mInsertOrReplacePendingFileAttachment == null) {
            mInsertOrReplacePendingFileAttachment = mDatabase.compileStatement(INSERT_OR_REPLACE_PENDING_FILE_ATTACHMENT);
        }
        mDatabase.beginTransaction();

        try {
            mInsertOrReplacePendingFileAttachment.bindString(1, pendingFileId);
            mInsertOrReplacePendingFileAttachment.bindString(2, messageId);
            mInsertOrReplacePendingFileAttachment.bindString(3, channelId);
            mInsertOrReplacePendingFileAttachment.bindLong(4, isOEmbed ? 1 : 0);
            mInsertOrReplacePendingFileAttachment.execute();
            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
            mInsertOrReplacePendingFileAttachment.clearBindings();
        }
    }

    public void insertOrReplaceMessage(MessagePlus messagePlus) {
        if(mInsertOrReplaceMessage == null) {
            mInsertOrReplaceMessage = mDatabase.compileStatement(INSERT_OR_REPLACE_MESSAGE);
            mInsertMessageSearchText = mDatabase.compileStatement(INSERT_MESSAGE_SEARCH_TEXT);
        }
        mDatabase.beginTransaction();

        Date displayDate = messagePlus.getDisplayDate();
        Message message = messagePlus.getMessage();
        String text = message.getText();
        message.setText(null);
        Long messageId = Long.valueOf(message.getId());

        try {
            mInsertOrReplaceMessage.bindLong(1, messageId);
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

            Map<String, PendingFileAttachment> attachments = messagePlus.getPendingFileAttachments();
            if(attachments != null) {
                for(String pendingFileId : attachments.keySet()) {
                    insertOrReplacePendingFileAttachment(pendingFileId, message.getId(), message.getChannelId(), attachments.get(pendingFileId).isOEmbed());
                }
            }

            insertSearchableMessageText(messageId, message.getChannelId(), text);
            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
            mInsertOrReplaceMessage.clearBindings();
            message.setText(text);
        }
    }

    private void insertSearchableMessageText(long messageId, String channelId, String text) {
        if(isFullTextSearchAvailable() && text != null) {
            mDatabase.beginTransaction();
            try {
                mInsertMessageSearchText.bindLong(1, messageId);
                mInsertMessageSearchText.bindString(2, channelId);
                mInsertMessageSearchText.bindString(3, text);
                mInsertMessageSearchText.execute();
                mDatabase.setTransactionSuccessful();
            } catch(Exception e) {
                Log.e(TAG, e.getMessage(), e);
            } finally {
                mDatabase.endTransaction();
                mInsertMessageSearchText.clearBindings();
            }
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

    public void insertOrReplacePlace(Place place) {
        if(mInsertOrReplacePlace == null) {
            mInsertOrReplacePlace = mDatabase.compileStatement(INSERT_OR_REPLACE_PLACE);
        }
        mDatabase.beginTransaction();
        try {
            double latitude = getRoundedValue(place.getLatitude(), 3);
            double longitude = getRoundedValue(place.getLongitude(), 3);
            boolean isCustomPlace = place instanceof CustomPlace;
            String id = isCustomPlace ? ((CustomPlace)place).getId() : place.getFactualId();

            mInsertOrReplacePlace.bindString(1, id);
            mInsertOrReplacePlace.bindString(2, place.getName());
            mInsertOrReplacePlace.bindDouble(3, latitude);
            mInsertOrReplacePlace.bindDouble(4, longitude);
            mInsertOrReplacePlace.bindLong(5, isCustomPlace ? 1 : 0);
            mInsertOrReplacePlace.bindString(6, AppDotNetGson.getPersistenceInstance().toJson(place));
            mInsertOrReplacePlace.execute();
            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
            mInsertOrReplacePlace.clearBindings();
        }
    }

    /**
     * Insert an instance of a DisplayLocation for the provided MessagePlus.
     *
     * @param messagePlus The MessagePlus objects whose DisplayLocation should be inserted
     */
    public void insertOrReplaceDisplayLocationInstance(MessagePlus messagePlus) {
        if(mInsertOrReplaceLocationInstance == null) {
            mInsertOrReplaceLocationInstance = mDatabase.compileStatement(INSERT_OR_REPLACE_LOCATION_INSTANCE);
            mInsertLocationInstanceSearchText = mDatabase.compileStatement(INSERT_LOCATION_INSTANCES_SEARCH_TEXT);
        }
        DisplayLocation location = messagePlus.getDisplayLocation();
        if(location != null) {
            Long messageId = Long.valueOf(messagePlus.getMessage().getId());
            String name = location.getName();
            String shortName = location.getShortName();
            String channelId = messagePlus.getMessage().getChannelId();
            String factualId = location.getFactualId();

            mDatabase.beginTransaction();
            try {
                mInsertOrReplaceLocationInstance.bindLong(1, messageId);
                mInsertOrReplaceLocationInstance.bindString(2, name);
                if(shortName != null) {
                    mInsertOrReplaceLocationInstance.bindString(3, shortName);
                } else {
                    mInsertOrReplaceLocationInstance.bindNull(3);
                }
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

                insertSearchableDisplayLocation(messageId, channelId, name);
                mDatabase.setTransactionSuccessful();
            } catch(Exception e) {
                Log.e(TAG, e.getMessage(), e);
            } finally {
                mDatabase.endTransaction();
                mInsertOrReplaceLocationInstance.clearBindings();
            }
        }
    }

    private void insertSearchableDisplayLocation(long messageId, String channelId, String locationName) {
        if(isFullTextSearchAvailable() && locationName != null) {
            mDatabase.beginTransaction();
            try {
                mInsertLocationInstanceSearchText.bindLong(1, messageId);
                mInsertLocationInstanceSearchText.bindString(2, channelId);
                mInsertLocationInstanceSearchText.bindString(3, locationName);
                mInsertLocationInstanceSearchText.execute();
                mDatabase.setTransactionSuccessful();
            } catch(Exception e) {
                Log.e(TAG, e.getMessage(), e);
            } finally {
                mDatabase.endTransaction();
                mInsertLocationInstanceSearchText.clearBindings();
            }
        }
    }

    public void insertOrReplaceAnnotationInstances(String annotationType, MessagePlus messagePlus) {
        if(mInsertOrReplaceAnnotationInstance == null) {
            mInsertOrReplaceAnnotationInstance = mDatabase.compileStatement(INSERT_OR_REPLACE_ANNOTATION_INSTANCE);
        }
        Message message = messagePlus.getMessage();
        if(message.hasAnnotations()) {
            List<Annotation> annotations = message.getAnnotationsOfType(annotationType);

            if(annotations.size() > 0) {
                mDatabase.beginTransaction();

                try {
                    mInsertOrReplaceAnnotationInstance.bindString(1, annotationType);
                    mInsertOrReplaceAnnotationInstance.bindString(2, message.getId());
                    mInsertOrReplaceAnnotationInstance.bindString(3, message.getChannelId());
                    mInsertOrReplaceAnnotationInstance.bindLong(4, annotations.size());
                    mInsertOrReplaceAnnotationInstance.bindLong(5, messagePlus.getDisplayDate().getTime());
                    mInsertOrReplaceAnnotationInstance.execute();
                    mDatabase.setTransactionSuccessful();
                } catch(Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                } finally {
                    mDatabase.endTransaction();
                    mInsertOrReplaceAnnotationInstance.clearBindings();
                }
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

    public void insertOrReplacePendingMessageDeletion(MessagePlus messagePlus, boolean deleteFiles) {
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

    /**
     * Insert a pending file deletion.
     *
     * @param fileId the id of the file that should be deleted.
     */
    public void insertOrReplacePendingFileDeletion(String fileId) {
        if(mInsertOrReplacePendingFileDeletion == null) {
            mInsertOrReplacePendingFileDeletion = mDatabase.compileStatement(INSERT_OR_REPLACE_PENDING_FILE_DELETION);
        }
        mDatabase.beginTransaction();

        try {
            mInsertOrReplacePendingFileDeletion.bindString(1, fileId);
            mInsertOrReplacePendingFileDeletion.execute();
            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
            mInsertOrReplacePendingFileDeletion.clearBindings();
        }
    }

    public void insertOrReplaceActionMessageSpec(MessagePlus actionMessagePlus, String targetMessageId, String targetChannelId, Date targetMessageDisplayDate) {
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
            mInsertOrReplaceActionMessageSpec.bindLong(5, targetMessageDisplayDate.getTime());
            mInsertOrReplaceActionMessageSpec.execute();
            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
            mInsertOrReplaceActionMessageSpec.clearBindings();
        }
    }

    public Set<String> getTargetMessageIdsWithSpecs(String actionChannelId, Collection<String> targetMessageIds) {
        Cursor cursor = null;
        HashSet<String> thoseWithSpecs = new HashSet<String>(targetMessageIds.size());
        try {
            String[] args = new String[targetMessageIds.size() + 1];
            args[0] = actionChannelId;
            String where = COL_ACTION_MESSAGE_CHANNEL_ID + " = ? AND " + COL_ACTION_MESSAGE_TARGET_MESSAGE_ID + " IN (";

            int index = 1;
            Iterator<String> iterator = targetMessageIds.iterator();
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
            cursor = mDatabase.query(TABLE_ACTION_MESSAGES, new String[] { COL_ACTION_MESSAGE_TARGET_MESSAGE_ID }, where, args, null, null, null, null);

            while(cursor.moveToNext()) {
                thoseWithSpecs.add(cursor.getString(0));
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return thoseWithSpecs;
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

    public int getActionMessageSpecCount(String actionChannelId) {
        Cursor cursor = null;
        try {
            String where = COL_ACTION_MESSAGE_CHANNEL_ID + " = ? ";
            String[] args = new String[] { actionChannelId };
            cursor = mDatabase.query(TABLE_ACTION_MESSAGES, new String[] { COL_ACTION_MESSAGE_ID }, where, args, null, null, null, null);
            if(cursor.moveToNext()) {
                int count = cursor.getCount();
                cursor.close();
                return count;
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    public List<ActionMessageSpec> getActionMessageSpecsForTargetMessages(List<String> targetMessageIds) {
        return getActionMessageSpecsForTargetMessages(null, targetMessageIds);
    }

    public List<ActionMessageSpec> getActionMessageSpecsForTargetMessages(String actionChannelId, List<String> targetMessageIds) {
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
        return getActionMessageSpecs(where, args, null, null);
    }

    public List<ActionMessageSpec> getActionMessageSpecsOrderedByTargetMessageDisplayDate(String actionChannelId, Integer limit) {
        return getActionMessageSpecsOrderedByTargetMessageDisplayDate(actionChannelId, null, limit);
    }

    public List<ActionMessageSpec> getActionMessageSpecsOrderedByTargetMessageDisplayDate(String actionChannelId, Date beforeDate, Integer limit) {
        String where = COL_ACTION_MESSAGE_CHANNEL_ID + " = ?";

        String[] args = null;
        String limitTo = limit != null ? limitTo = String.valueOf(limit) : null;

        if(beforeDate != null) {
            where += " AND " + "CAST(" + COL_ACTION_MESSAGE_TARGET_MESSAGE_DISPLAY_DATE + " AS INTEGER) < ?";
            args = new String[] { actionChannelId, String.valueOf(beforeDate.getTime()) };
        } else {
            args = new String[] { actionChannelId };
        }
        String orderBy = COL_ACTION_MESSAGE_TARGET_MESSAGE_DISPLAY_DATE + " DESC";
        return getActionMessageSpecs(where, args, orderBy, limitTo);
    }

    private List<ActionMessageSpec> getActionMessageSpecs(String where, String[] args, String orderBy, String limit) {
        ArrayList<ActionMessageSpec> actionMessageSpecs = new ArrayList<ActionMessageSpec>();
        Cursor cursor = null;
        try {
            cursor = mDatabase.query(TABLE_ACTION_MESSAGES, null, where, args, null, null, orderBy, limit);

            while(cursor.moveToNext()) {
                String aMessageId = cursor.getString(0);
                String aChannelId = cursor.getString(1);
                String tMessageId = cursor.getString(2);
                String tChannelId = cursor.getString(3);
                long tDisplayDate = cursor.getLong(4);

                ActionMessageSpec actionMessageSpec = new ActionMessageSpec(aMessageId, aChannelId, tMessageId, tChannelId, new Date(tDisplayDate));
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

    public List<PendingFileAttachment> getPendingFileAttachments(String messageId) {
        ArrayList<PendingFileAttachment> pendingAttachments = new ArrayList<PendingFileAttachment>();
        Cursor cursor = null;
        try {
            String where = COL_PENDING_FILE_ATTACHMENT_MESSAGE_ID + " = ?";
            String args[] = new String[] { messageId };
            cursor = mDatabase.query(TABLE_PENDING_FILE_ATTACHMENTS, new String[] {COL_PENDING_FILE_ATTACHMENT_PENDING_FILE_ID, COL_PENDING_FILE_ATTACHMENT_IS_OEMBED}, where, args, null, null, null, null);

            while(cursor.moveToNext()) {
                String pendingFileId = cursor.getString(0);
                boolean isOEmbed = cursor.getInt(1) == 1;
                pendingAttachments.add(new PendingFileAttachment(pendingFileId, isOEmbed));
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return pendingAttachments;
    }

    /**
     * Get an AnnotationInstances object representing the complete set of messages with an
     * Annotation of the specified type.
     *
     * @param channelId the Channel id
     * @param type The Annotation type
     *
     * @return AnnotationInstances
     */
    public AnnotationInstances getAnnotationInstances(String channelId, String type) {
        return getAnnotationInstances(channelId, type, null, null);
    }

    /**
     * Get an AnnotationInstances object representing the complete set of messages with an
     * Annotation of the specified type.
     *
     * @param channelId the Channel id
     * @param type The Annotation type
     * @param beforeDate a date that all display dates associated with the annotation instances must
     *                   come after. This is useful for paging. A null value is the same as passing
     *                   the current date.
     * @param limit The maximum number of instances to obtain. A null value means all will be returned.
     *
     * @return AnnotationInstances
     */
    public AnnotationInstances getAnnotationInstances(String channelId, String type, Date beforeDate, Integer limit) {
        Cursor cursor = null;
        AnnotationInstances instances = new AnnotationInstances(type);
        try {
            String where = COL_ANNOTATION_INSTANCE_CHANNEL_ID + " = ? AND " + COL_ANNOTATION_INSTANCE_TYPE + " = ?";
            String[] args = null;
            String limitTo = limit != null ? limitTo = String.valueOf(limit) : null;

            if(beforeDate != null) {
                where += " AND " + "CAST(" + COL_ANNOTATION_INSTANCE_DATE + " AS INTEGER) < ?";
                args = new String[] { channelId, type, String.valueOf(beforeDate.getTime()) };
            } else {
                args = new String[] { channelId, type };
            }

            String orderBy = COL_ANNOTATION_INSTANCE_DATE + " DESC";
            cursor = mDatabase.query(TABLE_ANNOTATION_INSTANCES, new String[] {COL_ANNOTATION_INSTANCE_MESSAGE_ID}, where, args, null, null, orderBy, limitTo);

            while(cursor.moveToNext()) {
                String messageId = cursor.getString(0);
                instances.addInstance(messageId);
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
     * This method uses a precision of LocationPrecision.TEN_THOUSAND_METERS (actually ~1.11 km) when
     * determining if two locations with the same name are considered equal.
     *
     * @param channelId the Channel id
     * @return a List of DisplaylocationInstances in descending order, from most to least recent
     */
    public List<DisplayLocationInstances> getDisplayLocationInstances(String channelId) {
        LinkedHashMap<String, DisplayLocationInstances> allInstances = getDisplayLocationInstancesMap(channelId);
        return Arrays.asList(allInstances.values().toArray(new DisplayLocationInstances[0]));
    }

    /**
     * Get all DisplayLocationInstances in the specified channel.
     *
     * This method uses a precision of LocationPrecision.TEN_THOUSAND_METERS (actually ~1.11 km) when
     * determining if two locations with the same name are considered equal.
     *
     * @param channelId the Channel id
     * @return a LinkedHashMap of DisplaylocationInstances in descending order, from most to least recent.
     *         The keys of this map are in the format: "name lat long" where lat and long are rounded to
     *         a single decimal point.
     *
     * @see com.alwaysallthetime.messagebeast.db.ADNDatabase#getDisplayLocationInstances(String)
     */
    public LinkedHashMap<String, DisplayLocationInstances> getDisplayLocationInstancesMap(String channelId) {
        LinkedHashMap<String, DisplayLocationInstances> allInstances = new LinkedHashMap<String, DisplayLocationInstances>();

        Cursor cursor = null;
        try {
            String where = COL_LOCATION_INSTANCE_CHANNEL_ID + " = ?";
            String args[] = new String[] { channelId };
            String orderBy = COL_LOCATION_INSTANCE_DATE + " DESC";

            String[] cols = new String[] { COL_LOCATION_INSTANCE_MESSAGE_ID, COL_LOCATION_INSTANCE_NAME, COL_LOCATION_INSTANCE_SHORT_NAME, COL_LOCATION_INSTANCE_LATITUDE, COL_LOCATION_INSTANCE_LONGITUDE };
            cursor = mDatabase.query(TABLE_LOCATION_INSTANCES, cols, where, args, null, null, orderBy, null);
            while(cursor.moveToNext()) {
                String messageId = cursor.getString(0);
                String name = cursor.getString(1);
                String shortName = cursor.getString(2);
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
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return allInstances;
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
     * @see com.alwaysallthetime.messagebeast.db.ADNDatabase#getDisplayLocationInstances(String, com.alwaysallthetime.messagebeast.model.DisplayLocation, com.alwaysallthetime.messagebeast.db.ADNDatabase.LocationPrecision)
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
        return getDisplayLocationInstances(channelId, location, precision, null, null);
    }

    /**
     * Get a DisplayLocationInstances object representing a set of messages with which
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
     * @param beforeDate a date that all display dates associated with the display location instances must
     *                   come after. This is useful for paging. A null value is the same as passing
     *                   the current date.
     * @param limit The maximum number of instances to obtain. A null value means all will be returned.
     * @return DisplayLocationInstances
     */
    public DisplayLocationInstances getDisplayLocationInstances(String channelId, DisplayLocation location, LocationPrecision precision, Date beforeDate, Integer limit) {
        Cursor cursor = null;
        DisplayLocationInstances instances = new DisplayLocationInstances(location);
        try {
            String where = COL_LOCATION_INSTANCE_CHANNEL_ID + " = ? AND " + COL_LOCATION_INSTANCE_NAME + " = ? AND " +
                    COL_LOCATION_INSTANCE_LATITUDE + " LIKE ? AND " + COL_LOCATION_INSTANCE_LONGITUDE + " LIKE ?";
            String limitTo = limit != null ? String.valueOf(limit) : null;

            String args[] = null;
            String latArg = null;
            String longArg = null;
            int precisionDigits = LocationPrecision.getNumPrecisionDigits(precision);

            latArg = String.format("%s%%", String.valueOf(getRoundedValue(location.getLatitude(), precisionDigits)));
            longArg = String.format("%s%%", String.valueOf(getRoundedValue(location.getLongitude(), precisionDigits)));

            if(beforeDate != null) {
                where += " AND " + "CAST(" + COL_LOCATION_INSTANCE_DATE + " AS INTEGER) < ?";
                args = new String[] { channelId, location.getName(), latArg, longArg, String.valueOf(beforeDate.getTime()) };
            } else {
                args = new String[] { channelId, location.getName(), latArg, longArg };
            }

            String orderBy = COL_LOCATION_INSTANCE_DATE + " DESC";

            String[] cols = new String[] { COL_LOCATION_INSTANCE_MESSAGE_ID };
            cursor = mDatabase.query(TABLE_LOCATION_INSTANCES, cols, where, args, null, null, orderBy, limitTo);
            while(cursor.moveToNext()) {
                String messageId = cursor.getString(0);
                instances.addInstance(messageId);
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
     * Get a Place by id.
     *
     * @param id the id corresponding to the Place. For App.net Places, this is the factual id,
     *           for CustomPlaces, this is the value returned by getId().
     * @return a Place if one with the specified factual id exists, null otherwise.
     */
    public Place getPlace(String id) {
        Place place = null;
        Cursor cursor = null;
        try {
            String where = COL_PLACE_ID + " = ?";

            Gson gson = AppDotNetGson.getPersistenceInstance();

            String[] args = new String[] { id };
            String[] cols = new String[] { COL_PLACE_IS_CUSTOM, COL_PLACE_JSON };
            cursor = mDatabase.query(TABLE_PLACES, cols, where, args, null, null, null, null);
            if(cursor.moveToNext()) {
                boolean isCustom = cursor.getInt(0) == 1;
                String json = cursor.getString(1);
                place = gson.fromJson(json, Place.class);
                if(isCustom) {
                    place = new CustomPlace(id, place);
                }
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return place;
    }

    /**
     * Get a List of CustomPlace objects whose names match the provided query.
     * This uses the full-text search virtual table corresponding to the location instances
     * to find matching names, so only places with display location instances in the db will
     * be returned (i.e. if you delete all messages that use a place, then this method will
     * not return that place, even if the Places table contains it).
     *
     * @param query The query to match against the name of the places.
     * @return a List of CustomPlace objects whose names match the provided query.
     */
    public List<CustomPlace> getCustomPlacesWithMatchingName(String query) {
        Cursor cursor = null;
        HashSet<String> placeNames = new HashSet<String>();
        try {
            String where = COL_LOCATION_INSTANCE_NAME + " MATCH ?";
            String[] args = new String[] { query };
            cursor = mDatabase.query(TABLE_LOCATION_INSTANCES_SEARCH, new String[] { COL_LOCATION_INSTANCE_NAME }, where, args, null, null, null, null);
            while(cursor.moveToNext()) {
                placeNames.add(cursor.getString(0));
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }

        ArrayList<CustomPlace> places = new ArrayList<CustomPlace>();
        if(placeNames.size() > 0){
            try {
                String where = COL_PLACE_IS_CUSTOM + " = ? AND " + COL_PLACE_NAME + " IN (";
                String[] args = new String[1+placeNames.size()];

                args[0] = "1";
                int index = 1;
                Iterator<String> iterator = placeNames.iterator();
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

                Gson gson = AppDotNetGson.getPersistenceInstance();

                String[] cols = new String[] { COL_PLACE_ID, COL_PLACE_JSON };
                cursor = mDatabase.query(TABLE_PLACES, cols, where, args, null, null, null, null);
                while(cursor.moveToNext()) {
                    String id = cursor.getString(0);
                    String json = cursor.getString(1);
                    Place place = gson.fromJson(json, Place.class);
                    places.add(new CustomPlace(id, place));
                }
            } catch(Exception e) {
                Log.e(TAG, e.getMessage(), e);
            } finally {
                if(cursor != null) {
                    cursor.close();
                }
            }
        }
        return places;
    }

    /**
     * Get a List of Places whose geocoordinates match the provided coordinate to a certain precision.
     *
     * Place longitude and latitude are stored by rounding to three decimal places. By providing a less
     * precise LocationPrecision, you can lookup by locations that match, e.g. 2 or 1 decimal places.
     *
     * @param latitude the latitude to match
     * @param longitude the longitude to match
     * @return a List of Place objects whose latitude and longitude match when the provided LocationPrecision
     * is applied.
     */
    public List<Place> getPlaces(double latitude, double longitude, LocationPrecision precision) {
        return getPlaces(latitude, longitude, precision, false);
    }

    /**
     * Get a List of Places whose geocoordinates match the provided coordinate to a certain precision,
     * optionally excluding custom places.
     *
     * Place longitude and latitude are stored by rounding to three decimal places. By providing a less
     * precise LocationPrecision, you can lookup by locations that match, e.g. 2 or 1 decimal places.
     *
     * @param latitude the latitude to match
     * @param longitude the longitude to match
     * @param excludeCustomPlaces true if custom places should be excluded, false otherwise.
     * @return a List of Place objects whose latitude and longitude match when the provided LocationPrecision
     * is applied.
     */
    public List<Place> getPlaces(double latitude, double longitude, LocationPrecision precision, boolean excludeCustomPlaces) {
        ArrayList<Place> places = new ArrayList<Place>();
        Cursor cursor = null;
        try {
            String where = COL_PLACE_ROUNDED_LATITUDE + " LIKE ? AND " + COL_PLACE_ROUNDED_LONGITUDE + " LIKE ?";

            Gson gson = AppDotNetGson.getPersistenceInstance();
            int precisionDigits = LocationPrecision.getNumPrecisionDigits(precision);
            String latArg = String.format("%s%%", String.valueOf(getRoundedValue(latitude, precisionDigits)));
            String longArg = String.format("%s%%", String.valueOf(getRoundedValue(longitude, precisionDigits)));

            String[] args = null;

            if(!excludeCustomPlaces) {
                args = new String[] { latArg, longArg };
            } else {
                where += " AND " + COL_PLACE_IS_CUSTOM + " = ?";
                args = new String[] { latArg, longArg, "0" };
            }

            String[] cols = new String[] { COL_PLACE_ID, COL_PLACE_IS_CUSTOM, COL_PLACE_JSON };
            cursor = mDatabase.query(TABLE_PLACES, cols, where, args, null, null, null, null);
            while(cursor.moveToNext()) {
                String id = cursor.getString(0);
                boolean isCustom = cursor.getInt(1) == 1;
                String json = cursor.getString(2);

                Place place = gson.fromJson(json, Place.class);
                if(!isCustom) {
                    places.add(place);
                } else {
                    places.add(new CustomPlace(id, place));
                }
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return places;
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
     * @see com.alwaysallthetime.messagebeast.db.ADNDatabase#getHashtagInstances(String, java.util.Date)
     */
    public LinkedHashMap<String, HashtagInstances> getHashtagInstances(String channelId, Date beforeDate, Date sinceDate) {
        LinkedHashMap<String, HashtagInstances> instances = new LinkedHashMap<String, HashtagInstances>();
        Cursor cursor = null;
        try {
            String where = COL_HASHTAG_INSTANCE_CHANNEL_ID + " =?";
            ArrayList<String> args = new ArrayList<String>(3);
            args.add(channelId);

            if(sinceDate != null) {
                where += " AND " + "CAST(" + COL_HASHTAG_INSTANCE_DATE + " AS INTEGER) >= ?";
                args.add(String.valueOf(sinceDate.getTime()));
            }
            if(beforeDate != null) {
                where += " AND " + "CAST(" + COL_HASHTAG_INSTANCE_DATE + " AS INTEGER) < ?";
                args.add(String.valueOf(beforeDate.getTime()));
            }

            String orderBy = COL_HASHTAG_INSTANCE_DATE + " DESC";
            cursor = mDatabase.query(TABLE_HASHTAG_INSTANCES, null, where, args.toArray(new String[0]), null, null, orderBy, null);

            while(cursor.moveToNext()) {
                String hashtag = cursor.getString(0);
                String messageId = cursor.getString(1);
                HashtagInstances c = instances.get(hashtag);
                if(c == null) {
                    c = new HashtagInstances(hashtag, messageId);
                    instances.put(hashtag, c);
                } else {
                    c.addInstance(messageId);
                }
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
        return getHashtagInstances(channelId, hashtagName, null, null);
    }

    /**
     * Get a HashtagInstances object representing Messages in which the specified
     * hashtag was used.
     *
     * @param channelId The id of the channel
     * @param hashtagName the hashtag for which instances should be retrieved.
     * @param beforeDate a date that all display dates associated with the hashtag instances must
     *                   come after. This is useful for paging. A null value is the same as passing
     *                   the current date.
     * @param limit The maximum number of instances to obtain. A null value means all will be returned.
     * @return HashtagInstances
     */
    public HashtagInstances getHashtagInstances(String channelId, String hashtagName, Date beforeDate, Integer limit) {
        Cursor cursor = null;
        HashtagInstances instances = new HashtagInstances(hashtagName);
        try {
            String where = COL_HASHTAG_INSTANCE_CHANNEL_ID + " =? AND " + COL_HASHTAG_INSTANCE_NAME + " = ?";
            String[] args = null;
            String theLimit = limit != null ? String.valueOf(limit) : null;

            if(beforeDate != null) {
                where += " AND " + "CAST(" + COL_HASHTAG_INSTANCE_DATE + " AS INTEGER) < ?";
                args = new String[] { channelId, hashtagName, String.valueOf(beforeDate.getTime()) };
            } else {
                args = new String[] { channelId, hashtagName };
            }

            String orderBy = COL_HASHTAG_INSTANCE_DATE + " DESC";
            cursor = mDatabase.query(TABLE_HASHTAG_INSTANCES, new String[] { COL_HASHTAG_INSTANCE_MESSAGE_ID }, where, args, null, null, orderBy, theLimit);

            while(cursor.moveToNext()) {
                String messageId = cursor.getString(0);
                instances.addInstance(messageId);
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
        Cursor cursor = null;
        HashSet<String> messageIds = new HashSet<String>();
        try {
            cursor = mDatabase.query(TABLE_MESSAGES_SEARCH, new String[] { "docid" }, where, args, null, null, null, null);
            while(cursor.moveToNext()) {
                messageIds.add(cursor.getString(0));
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }

        return getMessages(messageIds);
    }

    public OrderedMessageBatch searchForMessagesByDisplayLocation(String channelId, String query) {
        String where = COL_LOCATION_INSTANCE_CHANNEL_ID + " = ? AND " + COL_LOCATION_INSTANCE_NAME + " MATCH ?";
        String[] args = new String[] { channelId, query };
        Cursor cursor = null;
        HashSet<String> messageIds = new HashSet<String>();
        try {
            cursor = mDatabase.query(TABLE_LOCATION_INSTANCES_SEARCH, new String[] { "docid" }, where, args, null, null, null, null);
            while(cursor.moveToNext()) {
                messageIds.add(cursor.getString(0));
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }

        return getMessages(messageIds);
    }

    public MessagePlus getMessage(String messageId) {
        HashSet<String> ids = new HashSet<String>(1);
        ids.add(messageId);
        OrderedMessageBatch messages = getMessages(ids);
        TreeMap<Long, MessagePlus> orderedMessages = messages.getMessages();
        if(orderedMessages.size() == 1) {
            return orderedMessages.values().iterator().next();
        }
        return null;
    }

    /**
     * Get Messages by id.
     *
     * @param messageIds The ids of the Messages to get.
     * @return OrderedMessageBatch
     */
    public OrderedMessageBatch getMessages(Collection<String> messageIds) {
        String where = COL_MESSAGE_ID + " IN (";
        String[] args = new String[messageIds.size()];

        int index = 0;
        Iterator<String> iterator = messageIds.iterator();
        while(iterator.hasNext()) {
            args[index] = iterator.next();
            if(index > 0) {
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
            where += " AND " + "CAST(" + COL_MESSAGE_DATE + " AS INTEGER)" + " < ?";
            args = new String[] { channelId,  String.valueOf(beforeDate.getTime()) };
        } else {
            args = new String[] { channelId };
        }
        String orderBy = COL_MESSAGE_DATE + " DESC";
        return getMessages(where, args, orderBy, String.valueOf(limit));
    }

    private OrderedMessageBatch getMessages(String where, String[] args, String orderBy, String limit) {
        TreeMap<Long, MessagePlus> messages = new TreeMap<Long, MessagePlus>(new ReverseChronologicalComparator());
        ArrayList<MessagePlus> messagePlusses = new ArrayList<MessagePlus>();
        Long maxDate = null, minDate = null;
        Integer maxId = null, minId = null;

        Cursor cursor = null;
        try {
            cursor = mDatabase.query(TABLE_MESSAGES, null, where, args, null, null, orderBy, limit);

            Integer messageId = null;
            Long date = null;

            while(cursor.moveToNext()) {
                messageId = cursor.getInt(0);
                date = cursor.getLong(2);
                String messageJson = cursor.getString(3);
                String messageText = cursor.getString(4);
                boolean isUnsent = cursor.getInt(5) == 1;
                int numSendAttempts = cursor.getInt(6);
                Message message = mGson.fromJson(messageJson, Message.class);
                message.setText(messageText);

                MessagePlus messagePlus = new MessagePlus(message);
                messagePlus.setDisplayDate(new Date(date));
                messagePlus.setIsUnsent(isUnsent);
                messagePlus.setNumSendAttempts(numSendAttempts);
                messages.put(date, messagePlus);

                if(maxDate == null) {
                    maxDate = date;
                    maxId = messageId;
                    minId = messageId;
                } else {
                    //this must happen because id order is not necessarily same as date order
                    //(and we know the results are ordered by date)
                    maxId = Math.max(messageId, maxId);
                    minId = Math.min(messageId, minId);
                }

                //this is just for efficiency
                //if it is already sent, then we don't need to try to populate pending
                //file attachments.
                if(isUnsent) {
                    messagePlusses.add(messagePlus);
                }
            }

            //because they're ordered by recency, we know the last one will be the minDate
            minDate = date;

        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        populatePendingFileAttachments(messagePlusses);
        String minIdString = minId != null ? String.valueOf(minId) : null;
        String maxIdString = maxId != null ? String.valueOf(maxId) : null;
        return new OrderedMessageBatch(messages, new MinMaxPair(minIdString, maxIdString, minDate, maxDate));
    }

    /**
     * Get all messages marked as unsent in a channel.
     * 
     * Unlike other message getters, this one returns the messages in ascending order
     * (i.e. chronological order) - because this is the order in which they should be sent
     * to the server.
     *
     * @param channelId
     * @return a TreeMap with message times in millis as keys, mapped to MessagePlus objects.
     */
    public TreeMap<Long, MessagePlus> getUnsentMessages(String channelId) {
        TreeMap<Long, MessagePlus> unsentMessages = new TreeMap<Long, MessagePlus>();

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
                unsentMessages.put(date, messagePlus);
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        populatePendingFileAttachments(unsentMessages.values());
        return unsentMessages;
    }

    private void populatePendingFileAttachments(Collection<MessagePlus> messagePlusses) {
        for(MessagePlus messagePlus : messagePlusses) {
            Message message = messagePlus.getMessage();
            List<PendingFileAttachment> pendingAttachments = getPendingFileAttachments(message.getId());
            messagePlus.setPendingFileAttachments(pendingAttachments);
        }
    }

    public Set<String> getMessagesDependentOnPendingFile(String pendingFileId) {
        HashSet<String> messageIds = new HashSet<String>();

        Cursor cursor = null;
        try {
            String where = COL_PENDING_FILE_ATTACHMENT_PENDING_FILE_ID + " = ?";
            String args[] = new String[] { pendingFileId };
            String cols[] = { COL_PENDING_FILE_ATTACHMENT_MESSAGE_ID };
            cursor = mDatabase.query(TABLE_PENDING_FILE_ATTACHMENTS, cols, where, args, null, null, null, null);
            while(cursor.moveToNext()) {
                messageIds.add(cursor.getString(0));
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return messageIds;
    }

    /**
     * Get the ids of all pending file deletions.
     *
     * @return a Set of file ids corresponding to files that should be deleted.
     */
    public Set<String> getPendingFileDeletions() {
        HashSet<String> pendingFileDeletions = new HashSet<String>();

        Cursor cursor = null;
        try {
            cursor = mDatabase.query(TABLE_PENDING_FILE_DELETIONS, null, null, null, null, null, null, null);
            while(cursor.moveToNext()) {
                pendingFileDeletions.add(cursor.getString(0));
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }

        return pendingFileDeletions;
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

    /**
     * Delete all rows from all tables.
     */
    public void deleteAll() {
        mDatabase.delete(TABLE_ACTION_MESSAGES, null, null);
        mDatabase.delete(TABLE_ANNOTATION_INSTANCES, null, null);
        mDatabase.delete(TABLE_GEOLOCATIONS, null, null);
        mDatabase.delete(TABLE_HASHTAG_INSTANCES, null, null);
        mDatabase.delete(TABLE_LOCATION_INSTANCES, null, null);
        mDatabase.delete(TABLE_LOCATION_INSTANCES_SEARCH, null, null);
        mDatabase.delete(TABLE_MESSAGES, null, null);
        mDatabase.delete(TABLE_MESSAGES_SEARCH, null, null);
        mDatabase.delete(TABLE_PENDING_FILE_ATTACHMENTS, null, null);
        mDatabase.delete(TABLE_PENDING_FILE_DELETIONS, null, null);
        mDatabase.delete(TABLE_PENDING_FILES, null, null);
        mDatabase.delete(TABLE_PENDING_MESSAGE_DELETIONS, null, null);
        mDatabase.delete(TABLE_PLACES, null, null);
    }

    public void deleteMessage(MessagePlus messagePlus) {
        mDatabase.beginTransaction();

        try {
            Message message = messagePlus.getMessage();
            Long messageId = Long.valueOf(message.getId());
            mDatabase.delete(TABLE_MESSAGES_SEARCH, "docid=" + messageId, null);
            mDatabase.delete(TABLE_LOCATION_INSTANCES_SEARCH, "docid=" + messageId, null);
            mDatabase.delete(TABLE_MESSAGES, COL_MESSAGE_ID + " = " + messageId, null);

            deleteAnnotationInstances(message.getId());

            //this might be null in the case of unsent messages.
            Entities entities = message.getEntities();
            if(entities != null) {
                ArrayList<Entities.Hashtag> hashtags = entities.getHashtags();
                for(Entities.Hashtag h : hashtags) {
                    String where = COL_HASHTAG_INSTANCE_NAME + " = '" + h.getName() + "' AND " + COL_HASHTAG_INSTANCE_MESSAGE_ID + " = '" + message.getId() + "'";
                    mDatabase.delete(TABLE_HASHTAG_INSTANCES, where, null);
                }
            }

            String where = COL_LOCATION_INSTANCE_MESSAGE_ID + " = " + message.getId();
            mDatabase.delete(TABLE_LOCATION_INSTANCES, where, null);

            if(messagePlus.hasPendingFileAttachments()) {
                Map<String, PendingFileAttachment> pendingAttachments = messagePlus.getPendingFileAttachments();
                for(String pendingFileId : pendingAttachments.keySet()) {
                    deletePendingFileAttachment(pendingFileId, message.getId());

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

    public void deletePendingFileDeletion(String fileId) {
        mDatabase.delete(TABLE_PENDING_FILE_DELETIONS, COL_PENDING_FILE_DELETION_FILE_ID + " = '" + fileId + "'", null);
    }

    private void deleteAnnotationInstances(String type, String messageId) {
        String where = COL_ANNOTATION_INSTANCE_TYPE + " = " + "'" + type + "' AND " + COL_ANNOTATION_INSTANCE_MESSAGE_ID + " = " + "'" + messageId + "'";
        mDatabase.delete(TABLE_ANNOTATION_INSTANCES, where, null);
    }

    private void deleteAnnotationInstances(String messageId) {
        String where = COL_ANNOTATION_INSTANCE_MESSAGE_ID + " = " + "'" + messageId + "'";
        mDatabase.delete(TABLE_ANNOTATION_INSTANCES, where, null);
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

    public void deletePendingFileAttachment(String pendingFileId, String messageId) {
        mDatabase.beginTransaction();

        try {
            String where = COL_PENDING_FILE_ATTACHMENT_PENDING_FILE_ID + " = '" + pendingFileId + "' AND " +
                    COL_PENDING_FILE_ATTACHMENT_MESSAGE_ID + " = '" + messageId + "'";
            mDatabase.delete(TABLE_PENDING_FILE_ATTACHMENTS, where, null);
            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
        }
    }

    public void deletePlace(String factualId) {
        mDatabase.beginTransaction();

        try {
            String where = COL_PLACE_ID + " = '" + factualId + "'";
            mDatabase.delete(TABLE_PLACES, where, null);
            mDatabase.setTransactionSuccessful();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            mDatabase.endTransaction();
        }
    }

    public void deletePlaces() {
        mDatabase.beginTransaction();

        try {
            mDatabase.delete(TABLE_PLACES, null, null);
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
