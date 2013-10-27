package com.alwaysallthetime.adnlibutils.model;

import com.alwaysallthetime.adnlib.data.Annotation;

import java.util.HashMap;

public class DisplayLocation {
    private String mName;
    private String mFactualId;
    private double mLatitude;
    private double mLongitude;

    public static DisplayLocation fromCheckinAnnotation(Annotation checkin) {
        HashMap<String, Object> value = checkin.getValue();
        String locationName = (String) value.get("name");
        String factualId = (String) value.get("factual_id");
        DisplayLocation loc = new DisplayLocation(locationName, factualId, (Double)value.get("latitude"), (Double)value.get("longitude"));
        return loc;
    }

    public static DisplayLocation fromOhaiLocation(Annotation ohaiLocation) {
        HashMap<String, Object> value = ohaiLocation.getValue();
        String name = (String) value.get("name");
        Double latitude = (Double) value.get("latitude");
        Double longitude = (Double) value.get("longitude");
        if(name == null) {
            if(latitude != null && longitude != null) {
                name = getLatLongString(latitude, longitude);
            }
        }
        return new DisplayLocation(name, (Double)value.get("latitude"), (Double)value.get("longitude"));
    }

    public static DisplayLocation fromGeolocation(Geolocation geolocation) {
        return new DisplayLocation(geolocation.getName(), geolocation.getLatitude(), geolocation.getLongitude());
    }

    public DisplayLocation(double latitude, double longitude) {
        mLatitude = latitude;
        mLongitude = longitude;
    }

    public DisplayLocation(String name, double latitude, double longitude) {
        this(latitude, longitude);
        mName = name;
    }

    public DisplayLocation(String name, String factualId, double latitude, double longitude) {
        this(latitude, longitude);
        mName = name;
        mFactualId = factualId;
    }

    public String getName() {
        return mName;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public String getFactualId() {
        return mFactualId;
    }

    private static String getLatLongString(double latitude, double longitude) {
        return String.format("%s, %s", String.valueOf(latitude), String.valueOf(longitude));
    }
}
