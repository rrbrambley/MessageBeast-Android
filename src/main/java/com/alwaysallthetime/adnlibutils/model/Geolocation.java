package com.alwaysallthetime.adnlibutils.model;

public class Geolocation {
    private String mName;
    private double mLatitude;
    private double mLongitude;

    public Geolocation(String name, double latitude, double longitude) {
        mName = name;
        mLatitude = latitude;
        mLongitude = longitude;
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
}
