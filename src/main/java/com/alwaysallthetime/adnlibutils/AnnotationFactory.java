package com.alwaysallthetime.adnlibutils;

import com.alwaysallthetime.adnlib.Annotations;
import com.alwaysallthetime.adnlib.data.Annotation;
import com.alwaysallthetime.adnlib.data.File;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotationFactory {
    public static Annotation getCheckinAnnotation(String factualId) {
        HashMap<String, Object> value = new HashMap<String, Object>(1);
        HashMap<String, String> replacement = new HashMap<String, String>(1);
        replacement.put("factual_id", factualId);
        value.put(Annotations.REPLACEMENT_PLACE, replacement);
        Annotation a = new Annotation(Annotations.CHECKIN);
        a.setValue(value);
        return a;
    }

    public static Annotation getGeolocationAnnotation(double latitude, double longitude, float altitude) {
        HashMap<String, Object> value = new HashMap<String, Object>(2);
        value.put("latitude", Double.valueOf(latitude));
        value.put("longitude", Double.valueOf(longitude));
        if(altitude != 0) {
            value.put("altitude", Double.valueOf(altitude));
        }
        Annotation a = new Annotation(Annotations.GEOLOCATION);
        a.setValue(value);
        return a;
    }

    public static Annotation getOEmbedAnnotation(File file) {
        HashMap<String, Object> value = new HashMap<String, Object>(1);
        HashMap<String, String> replacement = new HashMap<String, String>(1);
        replacement.put("file_token", file.getFileToken());
        replacement.put("format", "oembed");
        replacement.put("file_id", file.getId());
        value.put(Annotations.REPLACEMENT_FILE, replacement);
        Annotation a = new Annotation(Annotations.OEMBED);
        a.setValue(value);
        return a;
    }

    public static Annotation getAttachmentsAnnotation(File file) {
        ArrayList<File> fileList = new ArrayList<File>(1);
        fileList.add(file);
        return getAttachmentsAnnotation(fileList);
    }

    public static Annotation getAttachmentsAnnotation(List<File> files) {
        HashMap<String, Object> value = new HashMap<String, Object>(1);

        ArrayList<HashMap<String, String>> fileList = new ArrayList<HashMap<String, String>>(files.size());
        for(File file : files) {
            HashMap<String, String> nextFile = new HashMap<String, String>(3);
            nextFile.put("file_token", file.getFileToken());
            nextFile.put("format", "metadata");
            nextFile.put("file_id", file.getId());
            fileList.add(nextFile);
        }
        value.put(Annotations.REPLACEMENT_FILE_LIST, fileList);
        Annotation a = new Annotation(Annotations.ATTACHMENTS);
        a.setValue(value);
        return a;
    }

    public static Annotation getGeolocationAnnotation(double latitude, double longitude) {
        return getGeolocationAnnotation(latitude, longitude, 0);
    }

    public static Annotation getOhaiDateAnnotation(String iso8601DateString) {
        HashMap<String, Object> value = new HashMap<String, Object>(1);
        value.put("date", iso8601DateString);
        Annotation a = new Annotation(Annotations.OHAI_DISPLAY_DATE);
        a.setValue(value);
        return a;
    }

    public static Annotation getSingleValueAnnotation(String type, String valueName, Object valueObject) {
        Annotation annotation = new Annotation(type);
        HashMap<String, Object> value = new HashMap<String, Object>(1);
        value.put(valueName, valueObject);
        annotation.setValue(value);
        return annotation;
    }

    /**
     * Get an Annotation of type net.app.ohai.displaydate with the provided Date
     *
     * @param date The Date to format for the display date Annotation
     * @return an Annotation of type net.app.ohai.displaydate with the provided Date
     */
    public static Annotation getDisplayDateAnnotation(Date date) {
        Annotation displayDateAnnotation = new Annotation(Annotations.OHAI_DISPLAY_DATE);
        HashMap<String, Object> value = new HashMap<String, Object>(1);
        value.put("date", AnnotationUtility.getIso8601StringfromDate(date));
        displayDateAnnotation.setValue(value);
        return displayDateAnnotation;
    }
}
