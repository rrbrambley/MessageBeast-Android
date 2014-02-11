package com.alwaysallthetime.messagebeast.model;

import com.alwaysallthetime.adnlib.data.Place;

/**
 * A CustomPlace is a Place with an id instead of a factual_id.
 */
public class CustomPlace extends Place {

    private String id;

    /**
     * Construct a new CustomPlace.
     *
     * @param id
     * @param place
     */
    public CustomPlace(String id, Place place) {
        this.id = id;

        this.name = place.getName();
        this.address = place.getAddress();
        this.addressExtended = place.getAddressExtended();
        this.locality = place.getLocality();
        this.region = place.getRegion();
        this.adminRegion = place.getAdminRegion();
        this.postTown = place.getPostTown();
        this.poBox = place.getPoBox();
        this.postcode = place.getPostcode();
        this.countryCode = place.getCountryCode();
        this.latitude = place.getLatitude();
        this.longitude = place.getLongitude();
        this.isOpen = place.isOpen();
        this.telephone = place.getTelephone();
        this.fax = place.getFax();
        this.website = place.getWebsite();
        this.categories = place.getCategories();
    }

    public String getId() {
        return id;
    }
}
