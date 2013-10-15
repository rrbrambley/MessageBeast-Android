package com.alwaysallthetime.adnlibutils.manager;

public class MinMaxPair {
    public String minId;
    public String maxId;

    public MinMaxPair() {}

    public MinMaxPair(String minId, String maxId) {
        this.minId = minId;
        this.maxId = maxId;
    }

    public MinMaxPair combine(MinMaxPair otherMinMaxPair) {
        Integer thisMin = minId != null ? Integer.parseInt(minId) : null;
        Integer thisMax = maxId != null ? Integer.parseInt(maxId) : null;
        Integer otherMin = otherMinMaxPair.minId != null ? Integer.parseInt(otherMinMaxPair.minId) : null;
        Integer otherMax = otherMinMaxPair.maxId != null ? Integer.parseInt(otherMinMaxPair.maxId) : null;

        String newMin = null;
        String newMax = null;
        if(thisMin != null && otherMin != null) {
            newMin = String.valueOf(Math.min(thisMin, otherMin));
        } else if(otherMin != null) {
            newMin = String.valueOf(otherMin);
        } else if(thisMin != null) {
            newMin = String.valueOf(thisMin);
        }

        if(thisMax != null && otherMax != null) {
            newMax = String.valueOf(Math.max(thisMax, otherMax));
        } else if(otherMax != null) {
            newMax = String.valueOf(otherMax);
        } else if(thisMax != null) {
            newMax = String.valueOf(thisMax);
        }

        return new MinMaxPair(newMin, newMax);
    }
}
