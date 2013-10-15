package com.alwaysallthetime.adnlibutils;

import com.alwaysallthetime.adnlib.Annotations;
import com.alwaysallthetime.adnlib.data.Annotation;
import com.alwaysallthetime.adnlib.data.File;

import java.util.HashMap;

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
        value.put("latitude", String.valueOf(latitude));
        value.put("longitude", String.valueOf(longitude));
        if(altitude != 0) {
            value.put("altitude", String.valueOf(altitude));
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
}
