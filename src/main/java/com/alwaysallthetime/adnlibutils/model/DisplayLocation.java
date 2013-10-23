package com.alwaysallthetime.adnlibutils.model;

import com.alwaysallthetime.adnlib.data.Annotation;

import java.util.HashMap;

public class DisplayLocation {
    private String mName;
    private double mLatitude;
    private double mLongitude;

    public static DisplayLocation fromCheckinAnnotation(Annotation checkin) {
        HashMap<String, Object> value = checkin.getValue();
        String locationName = (String) value.get("name");
        return new DisplayLocation(locationName, (Double)value.get("latitude"), (Double)value.get("longitude"));
    }

    public static DisplayLocation fromOhaiLocation(Annotation ohaiLocation) {
        HashMap<String, Object> value = ohaiLocation.getValue();
        String name = (String) value.get("name");
        Double latitude = (Double) value.get("latitude");
        Double longitude = (Double) value.get("longitude");
        if(name == null) {
            if(latitude != null && longitude != null) {
                name = getLatLongString(latitude.toString(), longitude.toString());
            }
        }
        return new DisplayLocation(name, (Double)value.get("latitude"), (Double)value.get("longitude"));
    }

    public DisplayLocation(double latitude, double longitude) {
        mLatitude = latitude;
        mLongitude = longitude;
    }

    public DisplayLocation(String name, double latitude, double longitude) {
        this(latitude, longitude);
        mName = name;
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

    private static String getLatLongString(String latitude, String longitude) {
        return String.format("%s, %s", latitude, longitude);
    }
}
