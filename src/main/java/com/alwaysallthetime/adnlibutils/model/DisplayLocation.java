package com.alwaysallthetime.adnlibutils.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.alwaysallthetime.adnlib.Annotations;
import com.alwaysallthetime.adnlib.data.Annotation;
import com.alwaysallthetime.adnlib.data.Place;
import com.alwaysallthetime.adnlibutils.db.ADNDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * A DisplayLocation represents a human-readable location that is attached to a Message. The main
 * goal of this class is to avoid having to handle several types of Annotations when preparing
 * Messages to display in UI.<br><br>
 *
 * A DisplayLocation is usually constructed via one of the following Annotations:<br><br>
 *
 * net.app.core.checkin <br>
 * net.app.ohai.location <br>
 * net.app.core.geolocation <br><br>
 *
 * In the case of a geolocation Annotation, reverse geocoding is performed prior to construction
 * of the DisplayLocation so that a name, and sometimes, a short name are available.<br><br>
 *
 * The MessageManager constructs DisplayLocations and attaches them to MessagePlus objects when
 * the MessageManagerConfiguration.isLocationLookupEnabled field is set to true (so, there is
 * probably no reason to be constructing these objects manually).
 *
 * @see com.alwaysallthetime.adnlibutils.manager.MessageManager.MessageManagerConfiguration#setLocationLookupEnabled(boolean)
 */
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

    /**
     * Construct a DisplayLocation from a net.app.core.checkin Annotation. This method also accepts
     * a replacement value (+net.app.core.place), provided the location with the associated
     * factual id has been persisted to the sqlite database.
     *
     * @param context a Context
     * @param checkin the checkin annotation
     * @return a DisplayLocation constructed by a server-returned checkin Annotation, or by a
     * checkin Annotation with a replacement value. This will return null if the factual_id value
     * in a replacement value is not associated with a Place in ADNDatabase.
     *
     * @see com.alwaysallthetime.adnlibutils.db.ADNDatabase#getPlace(String)
     * @see com.alwaysallthetime.adnlibutils.db.ADNDatabase#insertOrReplacePlace(com.alwaysallthetime.adnlib.data.Place)
     */
    public static DisplayLocation fromCheckinAnnotation(Context context, Annotation checkin) {
        HashMap<String, Object> value = checkin.getValue();

        Map<String, String> placeValue = (Map<String, String>) value.get(Annotations.REPLACEMENT_PLACE);
        if(placeValue != null) {
            //has not yet been sent to server
            String factualId = placeValue.get("factual_id");
            Place place = ADNDatabase.getInstance(context).getPlace(factualId);
            if(place != null) {
                DisplayLocation loc = new DisplayLocation(place.getName(), factualId, place.getLatitude(), place.getLongitude());
                loc.setType(LocationType.CHECKIN);
                return loc;
            }
        } else {
            //has been sent to server
            String locationName = (String) value.get("name");
            String factualId = (String) value.get("factual_id");
            DisplayLocation loc = new DisplayLocation(locationName, factualId, (Double)value.get("latitude"), (Double)value.get("longitude"));
            loc.setType(LocationType.CHECKIN);
            return loc;
        }
        return null;
    }

    /**
     * Construct a DisplayLocation from an Annotation of type net.app.ohai.location. This method
     * relies on the location having a value for the keys "name," "latitude," and "longitude." If
     * the name value is missing, then it is constructed in the format "latitude, longitude" (which
     * is ugly and stupid, so use annotations with a name value).
     *
     * @param ohaiLocation
     * @return a DisplayLocation constructed with the name, latitude, and longitude values from
     * an Annotation of type net.app.ohai.location.
     *
     * @see <a href="https://github.com/appdotnet/object-metadata/blob/master/annotations/net.app.ohai.location.md#custom-checkin">https://github.com/appdotnet/object-metadata/blob/master/annotations/net.app.ohai.location.md#custom-checkin</a>
     */
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

    /**
     * Construct a DisplayLocation from a Geolocation. Geolocations are generally constructed after
     * reverse geocoding is performed, which is done by the MessageManager when the
     * MessageManagerConfiguration.isLocationLookupEnabled field is set to true.
     *
     * @param geolocation the Geolocation object to use to construct a new DisplayLocation
     * @return a new DisplayLocation
     */
    public static DisplayLocation fromGeolocation(Geolocation geolocation) {
        DisplayLocation loc = new DisplayLocation(geolocation.getName(), geolocation.getLatitude(), geolocation.getLongitude());
        loc.setType(LocationType.GEOLOCATION);
        loc.setShortName(geolocation.getSubLocality());
        return loc;
    }

    /**
     * Construct a new DisplayLocation.
     *
     * @param name the name of the location
     * @param latitude the latitude coordinate of the location
     * @param longitude the longitude coordinate of the location
     */
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

    /**
     * Get the name of this DisplayLocation.
     *
     * @return the name of this DisplayLocation
     */
    public String getName() {
        return mName;
    }

    /**
     * Get the short name of this DisplayLocation, or null if it does not have one.
     * A short name is generally available when the MessageManager performs reverse geocoding
     * and obtains a subLocality (so, often when a Geolocation is used to construct the DisplayLocation).
     * For example, a DisplayLocation with the name "Mission District, San Francisco" might
     * have a short name of "Mission District."
     *
     * @return the short name for this DisplayLocation, or null if it does not have one.
     */
    public String getShortName() {
        return mShortName;
    }

    /**
     * Get the latitude value for this DisplayLocation.
     *
     * @return the latitude value for this DisplayLocation.
     */
    public double getLatitude() {
        return mLatitude;
    }

    /**
     * Get the longitude value for this DisplayLocation.
     *
     * @return the longitude value for this DisplayLocation.
     */
    public double getLongitude() {
        return mLongitude;
    }

    /**
     * Get the Factual id of this DisplayLocation. This is available when the DisplayLocation
     * is constructed from a net.app.core.checkin Annotation.
     *
     * @return the factual id associated with this DisplayLocation, or null if non exists
     */
    public String getFactualId() {
        return mFactualId;
    }

    /**
     * Set the short name of this DisplayLocation. The short name will typically be a substring
     * of the name that is more easily displayable in the user interface.
     *
     * @param shortName the short name to be used for this DisplayLocation
     */
    public void setShortName(String shortName) {
        mShortName = shortName;
    }

    /**
     * Get the LocationType that can be used to describe how this DisplayLocation was constructed.
     *
     * @return the LocationType used to describe how this DisplayLocation was constructed
     */
    public LocationType getType() {
        return mType;
    }

    private static String getLatLongString(double latitude, double longitude) {
        return String.format("%s, %s", String.valueOf(latitude), String.valueOf(longitude));
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
