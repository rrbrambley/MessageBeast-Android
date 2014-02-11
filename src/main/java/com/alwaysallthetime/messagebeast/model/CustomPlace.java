package com.alwaysallthetime.messagebeast.model;

import com.alwaysallthetime.adnlib.data.Annotation;
import com.alwaysallthetime.adnlib.data.Category;
import com.alwaysallthetime.adnlib.data.Place;
import com.alwaysallthetime.adnlib.gson.AppDotNetGson;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A CustomPlace is a wrapper for a Place that has an id
 * instead of a factual_id.
 */
public class CustomPlace extends Place {

    private String mId;
    private Place mPlace;

    /**
     * Construct a new CustomPlace.
     *
     * @param id
     * @param place
     */
    public CustomPlace(String id, Place place) {
        mId = id;
        mPlace = place;
    }

    public CustomPlace(Annotation ohaiLocationAnnotation) {
        HashMap<String,Object> value = ohaiLocationAnnotation.getValue();

        Gson gson = AppDotNetGson.getPersistenceInstance();
        String placeJson = gson.toJson(value);

        mPlace = gson.fromJson(placeJson, Place.class);
        mId = (String) value.get("id");
    }

    public String getId() {
        return mId;
    }

    public String getFactualId() {
        return null;
    }

    public String getName() {
        return mPlace.getName();
    }

    public String getAddress() {
        return mPlace.getAddress();
    }

    public String getAddressExtended() {
        return mPlace.getAddressExtended();
    }

    public String getLocality() {
        return mPlace.getLocality();
    }

    public String getRegion() {
        return mPlace.getRegion();
    }

    public String getAdminRegion() {
        return mPlace.getAdminRegion();
    }

    public String getPostTown() {
        return mPlace.getPostTown();
    }

    public String getPoBox() {
        return mPlace.getPoBox();
    }

    public String getPostcode() {
        return mPlace.getPostcode();
    }

    public String getCountryCode() {
        return mPlace.getCountryCode();
    }

    public double getLatitude() {
        return mPlace.getLatitude();
    }

    public double getLongitude() {
        return mPlace.getLongitude();
    }

    public boolean isOpen() {
        return mPlace.isOpen();
    }

    public String getTelephone() {
        return mPlace.getTelephone();
    }

    public String getFax() {
        return mPlace.getFax();
    }

    public String getWebsite() {
        return mPlace.getWebsite();
    }

    public ArrayList<Category> getCategories() {
        return mPlace.getCategories();
    }
}
