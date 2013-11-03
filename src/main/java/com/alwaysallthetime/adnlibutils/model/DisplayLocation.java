package com.alwaysallthetime.adnlibutils.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.alwaysallthetime.adnlib.data.Annotation;

import java.util.HashMap;

public class DisplayLocation implements Parcelable {
    public enum LocationType {
        CHECKIN,
        OHAI,
        GEOLOCATION,
        UNKNOWN;

        public static LocationType fromInt(int ordinal) {
            switch(ordinal) {
                case 0:
                    return CHECKIN;
                case 1:
                    return OHAI;
                case 2:
                    return GEOLOCATION;
                default:
                    return UNKNOWN;
            }
        }
    }

    private LocationType mType;
    private String mName;
    private String mShortName;
    private String mFactualId;
    private double mLatitude;
    private double mLongitude;

    public static DisplayLocation fromCheckinAnnotation(Annotation checkin) {
        HashMap<String, Object> value = checkin.getValue();
        String locationName = (String) value.get("name");
        String factualId = (String) value.get("factual_id");
        DisplayLocation loc = new DisplayLocation(locationName, factualId, (Double)value.get("latitude"), (Double)value.get("longitude"));
        loc.setType(LocationType.CHECKIN);
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
        DisplayLocation loc = new DisplayLocation(name, (Double)value.get("latitude"), (Double)value.get("longitude"));
        loc.setType(LocationType.OHAI);
        return loc;
    }

    public static DisplayLocation fromGeolocation(Geolocation geolocation) {
        DisplayLocation loc = new DisplayLocation(geolocation.getName(), geolocation.getLatitude(), geolocation.getLongitude());
        loc.setType(LocationType.GEOLOCATION);
        loc.setShortName(geolocation.getSubLocality());
        return loc;
    }

    public DisplayLocation(String name, double latitude, double longitude) {
        mLatitude = latitude;
        mLongitude = longitude;
        mName = name;
        mType = LocationType.UNKNOWN;
    }

    protected void setType(LocationType type) {
        mType = type;
    }

    protected DisplayLocation(String name, String factualId, double latitude, double longitude) {
        this(name, latitude, longitude);
        mFactualId = factualId;
    }

    public String getName() {
        return mName;
    }

    public String getShortName() {
        return mShortName;
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

    public void setShortName(String shortName) {
        mShortName = shortName;
    }

    private static String getLatLongString(double latitude, double longitude) {
        return String.format("%s, %s", String.valueOf(latitude), String.valueOf(longitude));
    }

    public LocationType getType() {
        return mType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeDouble(mLatitude);
        dest.writeDouble(mLongitude);
        dest.writeInt(mType.ordinal());
        if(mFactualId != null) {
            dest.writeInt(1);
            dest.writeString(mFactualId);
        } else {
            dest.writeInt(0);
        }
    }

    public final static Parcelable.Creator<DisplayLocation> CREATOR = new Parcelable.Creator<DisplayLocation>() {

        @Override
        public DisplayLocation createFromParcel(Parcel source) {
            String name = source.readString();
            double latitude= source.readDouble();
            double longitude = source.readDouble();
            int type = source.readInt();
            int hasFactualId = source.readInt();
            String factualId = null;
            if(hasFactualId == 1) {
                factualId = source.readString();
            }
            DisplayLocation loc = new DisplayLocation(name, factualId, latitude, longitude);
            loc.setType(LocationType.fromInt(type));
            return loc;
        }

        @Override
        public DisplayLocation[] newArray(int size) {
            return new DisplayLocation[size];
        }
    };
}
