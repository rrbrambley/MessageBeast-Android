package com.alwaysallthetime.adnlibutils.model;

public class Geolocation {
    private String mLocality;
    private String mSubLocality;
    private double mLatitude;
    private double mLongitude;

    public Geolocation(String locality, String subLocality, double latitude, double longitude) {
        mLocality = locality;
        mSubLocality = subLocality;
        mLatitude = latitude;
        mLongitude = longitude;
    }

    public String getName() {
        if(mSubLocality != null) {
            return String.format("%s, %s", mSubLocality, mLocality);
        } else {
            return mLocality;
        }
    }

    public String getLocality() {
        return mLocality;
    }

    public String getSubLocality() {
        return mSubLocality;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }
}
