package com.alwaysallthetime.messagebeast;

import android.content.Context;
import android.util.Log;

import com.alwaysallthetime.adnlib.Annotations;
import com.alwaysallthetime.adnlib.data.Annotation;
import com.alwaysallthetime.adnlib.data.Channel;
import com.alwaysallthetime.adnlib.data.File;
import com.alwaysallthetime.adnlib.data.Message;
import com.alwaysallthetime.adnlib.data.Place;
import com.alwaysallthetime.adnlib.gson.AppDotNetGson;
import com.alwaysallthetime.messagebeast.db.ADNDatabase;
import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * A Utility for obtaining data from Annotations on App.net resources.
 */
public class AnnotationUtility {
    private static final String TAG = "MessageBeast_AnnotationUtility";

    public static final String OEMBED_TYPE_PHOTO = "photo";
    public static final String OEMBED_TYPE_HTML5VIDEO = "html5video";

    private static SimpleDateFormat mIso8601Format;
    private static SimpleDateFormat mIso8601WithMillisFormat;

    /**
     * Get the date specified by an Annotation of type net.app.ohai.displaydate, if it exists.
     * If no display date annotation exists, then the provided Message's created_at date is returned.
     *
     * @param message The Message from which the display date should be obtained.
     * @return a Date corresponding to the value of the Ohai display date, or the Message's created_at
     * date if no net.app.ohai.displaydate annotation exists.
     */
    public static Date getOhaiDisplayDate(Message message) {
        Annotation displayDate = message.getFirstAnnotationOfType(Annotations.OHAI_DISPLAY_DATE);
        if(displayDate != null) {
            String date = (String) displayDate.getValue().get("date");
            if(date != null) {
                return getDateFromIso8601String(date);
            }
        }
        return message.getCreatedAt();
    }

    /**
     * Get a Date object corresponding to the provided ISO 8601 string
     *
     * @param date the date string
     * @return a Date object corresponding to the provided ISO 8601 string, or null
     * if the string cannot be parsed.
     */
    public static Date getDateFromIso8601String(String date) {
        initFormatter();
        try {
            return mIso8601WithMillisFormat.parse(date);
        } catch(ParseException e) {
            try {
                return mIso8601Format.parse(date);
            } catch(ParseException e1) {
                Log.e(TAG, e1.getMessage(), e1);
            }
        }
        return null;
    }

    /**
     * Get a date string in ISO 8601 format.
     *
     * @param date the Date object to convert to a String
     * @return a date string in ISO 8601 format.
     */
    public static String getIso8601StringfromDate(Date date) {
        initFormatter();
        return mIso8601WithMillisFormat.format(date);
    }

    /**
     * Get the first-found OEmbed Annotation of type "photo," if one exists, or null otherwise.
     *
     * @param message the Message from which the photo OEmbed Annotation should be obtained.
     * @return the first OEmbed Annotation of type "photo," if one exists, or null otherwise.
     */
    public static Annotation getFirstOEmbedPhotoAnnotation(Message message) {
        Annotation oEmbedAnnotation = message.getFirstAnnotationOfType(Annotations.OEMBED);
        if(oEmbedAnnotation != null && OEMBED_TYPE_PHOTO.equals(oEmbedAnnotation.getValue().get("type"))) {
            return oEmbedAnnotation;
        }
        return null;
    }

    /**
     * Get the URL of the first-found OEmbed Annotation of type "photo," if one exists, or null otherwise.
     *
     * @param message the Message from which the OEmbed photo URL should be obtained.
     * @return the URL of the first OEmbed Annotation of type "photo," if one exists, or null otherwise.
     */
    public static String getFirstOEmbedPhotoUrl(Message message) {
        Annotation oEmbedAnnotation = getFirstOEmbedPhotoAnnotation(message);
        if(oEmbedAnnotation != null) {
            return (String) oEmbedAnnotation.getValue().get("url");
        }
        return null;
    }

    /**
     * Get the source URL of the first-found OEmbed Annotation of type "html5video," if one exists, or null otherwise.
     *
     * @param message the Message from which the OEmbed html5video URL should be obtained.
     * @return the source URL of the first OEmbed Annotation of type "h5ml5video," if one exists, or null otherwise.
     */
    public static String getFirstHTML5VideoSource(Message message) {
        Annotation oEmbedAnnotation = message.getFirstAnnotationOfType(Annotations.OEMBED);
        if(OEMBED_TYPE_HTML5VIDEO.equals(oEmbedAnnotation.getValue().get("type"))) {
            List<Map<String, String>> sources = (List<Map<String, String>>) oEmbedAnnotation.getValue().get("sources");
            if(sources != null && sources.size() > 0) {
                Map<String, String> firstSource = sources.get(0);
                String url = firstSource.get("url");
                if(url != null) {
                    return url;
                }
            }
        }
        return null;
    }

    /**
     * Append a file to an Attachments annotation. This method assumes
     * that the annotation has an existing +net.app.core.file_list key.
     *
     * @param attachments The Attachments annotation to which the File should be appended
     * @param file The file to append
     */
    public static void appendFileToAttachmentsFileList(Annotation attachments, File file) {
        List<Map<String, String>> fileList = (List<Map<String, String>>) attachments.getValue().get(Annotations.REPLACEMENT_FILE_LIST);
        HashMap<String, String> nextFile = new HashMap<String, String>(3);
        nextFile.put("file_token", file.getFileToken());
        nextFile.put("format", "metadata");
        nextFile.put("file_id", file.getId());
        fileList.add(nextFile);
    }

    /**
     * Get the action_type value from a Channel's com.alwaysallthetime.action.metadata Annotation.
     *
     * @param channel The Channel for which the action type should be obtained
     * @return the action_type value from a Channel's com.alwaysallthetime.action.metadata Annotation,
     * or null if no such Annotation exists
     */
    public static String getActionChannelType(Channel channel) {
        Annotation a = channel.getFirstAnnotationOfType(PrivateChannelUtility.CHANNEL_ANNOTATION_TYPE_METADATA);
        if(a != null) {
            return (String) a.getValue().get(PrivateChannelUtility.ACTION_METADATA_KEY_ACTION_TYPE);
        }
        return null;
    }

    /**
     * Get the channel_id value from a Channel's com.alwaysallthetime.action.metadata Annotation.
     *
     * @param actionChannel the Channel from which the target channel id should be obtained
     * @return the channel_id value from a Channel's com.alwaysallthetime.action.metadata Annotation,
     * or null if no such Annotation exists
     */
    public static String getTargetChannelId(Channel actionChannel) {
        Annotation a = actionChannel.getFirstAnnotationOfType(PrivateChannelUtility.CHANNEL_ANNOTATION_TYPE_METADATA);
        if(a != null) {
            return (String) a.getValue().get(PrivateChannelUtility.ACTION_METADATA_KEY_TARGET_CHANNEL_ID);
        }
        return null;
    }

    /**
     * Get the id value from the provided Message's com.alwaysallthetime.action.target_message Annotation.
     *
     * @param actionMessage the Message from which the target Message id should be obtained
     * @return the id value from the provided Message's com.alwaysallthetime.action.target_message Annotation,
     * or null if no such Annotation exists
     */
    public static String getTargetMessageId(Message actionMessage) {
        Annotation targetMessage = actionMessage.getFirstAnnotationOfType(PrivateChannelUtility.MESSAGE_ANNOTATION_TARGET_MESSAGE);
        if(targetMessage != null) {
            return (String) targetMessage.getValue().get(PrivateChannelUtility.TARGET_MESSAGE_KEY_ID);
        }
        return null;
    }

    /**
     * Construct a Place from a net.app.core.checkin Annotation. This method also accepts
     * a replacement value (+net.app.core.place), provided the location with the associated
     * factual id has been persisted to the sqlite database. Null will be returned if no valid
     * Place can be constructed.
     *
     * @param context a Context
     * @param annotation the checkin annotation
     *
     * @return a Place constructed from the provided checkin Annotation, or null if one cannot
     * be constructed.
     */
    public static Place getPlaceFromCheckinAnnotation(Context context, Annotation annotation) {
        Place place = null;

        HashMap<String, Object> value = annotation.getValue();
        Map<String, String> placeValue = (Map<String, String>) value.get(Annotations.REPLACEMENT_PLACE);
        if(placeValue != null) {
            String factualId = placeValue.get("factual_id");
            place = ADNDatabase.getInstance(context).getPlace(factualId);
        } else {
            Gson gson = AppDotNetGson.getPersistenceInstance();
            String placeJson = gson.toJson(value);
            place = gson.fromJson(placeJson, Place.class);
        }
        return place;
    }

    private static void initFormatter() {
        if(mIso8601WithMillisFormat == null) {
            mIso8601WithMillisFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            mIso8601WithMillisFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            mIso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            mIso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
    }
}
