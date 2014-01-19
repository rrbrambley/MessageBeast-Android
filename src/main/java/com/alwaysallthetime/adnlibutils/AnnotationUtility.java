package com.alwaysallthetime.adnlibutils;

import android.util.Log;

import com.alwaysallthetime.adnlib.Annotations;
import com.alwaysallthetime.adnlib.data.Annotation;
import com.alwaysallthetime.adnlib.data.Channel;
import com.alwaysallthetime.adnlib.data.File;
import com.alwaysallthetime.adnlib.data.Message;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by brambley on 10/9/13.
 */
public class AnnotationUtility {
    private static final String TAG = "ADNLibUtils_AnnotationUtility";

    public static final String OEMBED_TYPE_PHOTO = "photo";
    public static final String OEMBED_TYPE_HTML5VIDEO = "html5video";

    private static SimpleDateFormat mIso8601Format;


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

    public static Date getDateFromIso8601String(String date) {
        initFormatter();
        try {
            return mIso8601Format.parse(date);
        } catch(ParseException e) {
            Log.d(TAG, e.getMessage(), e);
        }
        return null;
    }

    public static String getIso8601StringfromDate(Date date) {
        initFormatter();
        return mIso8601Format.format(date);
    }

    public static Annotation newDisplayDateAnnotation(Date date) {
        Annotation displayDateAnnotation = new Annotation(Annotations.OHAI_DISPLAY_DATE);
        HashMap<String, Object> value = new HashMap<String, Object>(1);
        value.put("date", getIso8601StringfromDate(date));
        displayDateAnnotation.setValue(value);
        return displayDateAnnotation;
    }

    public static Annotation getFirstOEmbedPhotoAnnotation(Message message) {
        Annotation oEmbedAnnotation = message.getFirstAnnotationOfType(Annotations.OEMBED);
        if(oEmbedAnnotation != null && OEMBED_TYPE_PHOTO.equals(oEmbedAnnotation.getValue().get("type"))) {
            return oEmbedAnnotation;
        }
        return null;
    }

    public static String getFirstOEmbedPhotoUrl(Message message) {
        Annotation oEmbedAnnotation = getFirstOEmbedPhotoAnnotation(message);
        if(oEmbedAnnotation != null) {
            return (String) oEmbedAnnotation.getValue().get("url");
        }
        return null;
    }

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

    public static void appendFileToAttachmentsFileList(Annotation attachments, File file) {
        List<Map<String, String>> fileList = (List<Map<String, String>>) attachments.getValue().get(Annotations.REPLACEMENT_FILE_LIST);
        HashMap<String, String> nextFile = new HashMap<String, String>(3);
        nextFile.put("file_token", file.getFileToken());
        nextFile.put("format", "metadata");
        nextFile.put("file_id", file.getId());
        fileList.add(nextFile);
    }

    public static String getActionChannelType(Channel channel) {
        Annotation a = channel.getFirstAnnotationOfType(PrivateChannelUtility.CHANNEL_ANNOTATION_TYPE_METADATA);
        if(a != null) {
            return (String) a.getValue().get(PrivateChannelUtility.ACTION_METADATA_KEY_ACTION_TYPE);
        }
        return null;
    }

    public static String getTargetChannelId(Channel actionChannel) {
        Annotation a = actionChannel.getFirstAnnotationOfType(PrivateChannelUtility.CHANNEL_ANNOTATION_TYPE_METADATA);
        if(a != null) {
            return (String) a.getValue().get(PrivateChannelUtility.ACTION_METADATA_KEY_TARGET_CHANNEL_ID);
        }
        return null;
    }

    public static String getTargetMessageId(Message actionMessage) {
        Annotation targetMessage = actionMessage.getFirstAnnotationOfType(PrivateChannelUtility.MESSAGE_ANNOTATION_TARGET_MESSAGE);
        if(targetMessage != null) {
            return (String) targetMessage.getValue().get(PrivateChannelUtility.TARGET_MESSAGE_KEY_ID);
        }
        return null;
    }

    private static void initFormatter() {
        if(mIso8601Format == null) {
            mIso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            mIso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
    }
}
