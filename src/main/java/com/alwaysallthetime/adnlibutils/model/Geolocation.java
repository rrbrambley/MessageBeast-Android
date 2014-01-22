package com.alwaysallthetime.adnlibutils.model;

/**
 * Geolocation is a class used to contain a human-readable name, locality, and sub-locality for
 * a set of geocoordinates. This is most often constructed after reverse geocoding coordinates to
 * obtain a name.
 */
public class Geolocation {
    private String mLocality;
    private String mSubLocality;
    private double mLatitude;
    private double mLongitude;

    /**
     * Construct a new Geolocation.
     *
     * @param locality the locality (city, etc.)
     * @param subLocality the sub-locality (neighborhood, etc.). Can be null.
     * @param latitude the latitude coordinate
     * @param longitude the longitude coordinate
     */
    public Geolocation(String locality, String subLocality, double latitude, double longitude) {
        mLocality = locality;
        mSubLocality = subLocality;
        mLatitude = latitude;
        mLongitude = longitude;
    }

    /**
     * Get the name of this Geolocation. If a sub-locality exists, then the locality is
     * concatenated to the sub-locality to form a name. Otherwise, the locality is returned.
     *
     * @return the name of this Geolocation
     */
    public String getName() {
        if(mSubLocality != null) {
            return String.format("%s, %s", mSubLocality, mLocality);
        } else {
            return mLocality;
        }
    }

    /**
     * Get the locality (e.g. city) for this Geolocation.
     *
     * @return the locality for this Geolocation
     */
    public String getLocality() {
        return mLocality;
    }

    /**
     * Get the sub-locality (e.g. neighborhood) for this Geolocation. May be null.
     *
     * @return the sub-locality for this Geolocation
     */
    public String getSubLocality() {
        return mSubLocality;
    }

    /**
     * Get the latitude coordinate for this Geolocation.
     *
     * @return the latitude coordinate for this Geolocation
     */
    public double getLatitude() {
        return mLatitude;
    }

    /**
     * Get the longitude coordinate for this Geolocation.
     *
     * @return the longitude coordinate for this Geolocation
     */
    public double getLongitude() {
        return mLongitude;
    }
}
