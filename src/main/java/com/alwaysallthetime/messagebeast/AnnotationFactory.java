package com.alwaysallthetime.messagebeast;

import com.alwaysallthetime.adnlib.Annotations;
import com.alwaysallthetime.adnlib.data.Annotation;
import com.alwaysallthetime.adnlib.data.File;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * A factory for Annotations.
 */
public class AnnotationFactory {

    /**
     * Get an Annotation of type net.app.core.checkin with the given Factual id.
     *
     * The Annotation will have a +net.app.core.place key whose associated Map
     * will contain a "factual_id" key/value pair.
     *
     * @param factualId the Factual id to embed in the checkin annotation.
     * @return an Annotation of type net.app.core.checkin with the given Factual id.
     */
    public static Annotation getCheckinAnnotation(String factualId) {
        HashMap<String, Object> value = new HashMap<String, Object>(1);
        HashMap<String, String> replacement = new HashMap<String, String>(1);
        replacement.put("factual_id", factualId);
        value.put(Annotations.REPLACEMENT_PLACE, replacement);
        Annotation a = new Annotation(Annotations.CHECKIN);
        a.setValue(value);
        return a;
    }

    /**
     * Get an Annotation of type net.app.core.geolocation with the provided latitude and longitude.
     *
     * No altitude value will be set.
     *
     * @param latitude
     * @param longitude
     * @return an Annotation of type net.app.core.geolocation
     */
    public static Annotation getGeolocationAnnotation(double latitude, double longitude) {
        return getGeolocationAnnotation(latitude, longitude, 0);
    }

    /**
     * Get an Annotation of type net.app.core.geolocation with the provided latitude, longitude, and altitude.
     *
     * Providing an altitude value of 0 will result in an ommitted value for altitude.
     *
     * @param latitude
     * @param longitude
     * @param altitude
     * @return an Annotation of type net.app.core.geolocation
     */
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

    /**
     * Get an OEmbed Annotation with a +net.app.core.file replacement key mapped to
     * values from the provided File object
     *
     * @param file The file object whose token and id should be contained in the replacement value
     * @return an OEmbed Annotation with a +net.app.core.file replacement key mapped to
     * values from the provided File object
     */
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

    /**
     * Get an Annotation of type net.app.core.attachments, containing a +net.app.core.file_list
     * with the provided File
     *
     * @param file the File to add to the +net.app.core.file_list list in the attachment Annotation
     * @return an Annotation of type net.app.core.attachments, containing a +net.app.core.file_list
     * with the provided File
     */
    public static Annotation getAttachmentsAnnotation(File file) {
        ArrayList<File> fileList = new ArrayList<File>(1);
        fileList.add(file);
        return getAttachmentsAnnotation(fileList);
    }

    /**
     * Get an Annotation of type net.app.core.attachments, containing a +net.app.core.file_list
     * with the provided Files
     *
     * @param files a List of File objects to add to the +net.app.core.file_list list in the attachment Annotation
     * @return an Annotation of type net.app.core.attachments, containing a +net.app.core.file_list
     * with the provided Files
     */
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

    /**
     * Get an Annotation of type net.app.ohai.displaydate containing the provided ISO 8601 formatted date.
     *
     * @param iso8601DateString the ISO 8601 formatted date to place in the display date Annotation.
     * @return an Annotation of type net.app.ohai.displaydate containing the provided ISO 8601 formatted date.
     */
    public static Annotation getOhaiDisplayDateAnnotation(String iso8601DateString) {
        HashMap<String, Object> value = new HashMap<String, Object>(1);
        value.put("date", iso8601DateString);
        Annotation a = new Annotation(Annotations.OHAI_DISPLAY_DATE);
        a.setValue(value);
        return a;
    }

    /**
     * Get a new Annotation whose value Map contains a single key-value pair.
     *
     * @param type the Annotation type
     * @param valueName the key in the value Map
     * @param valueObject the value Object that is mapped to the valueName
     * @return a new Annotation whose value Map contains a single key-value pair.
     */
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
