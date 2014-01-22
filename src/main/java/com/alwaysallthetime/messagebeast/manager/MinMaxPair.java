package com.alwaysallthetime.messagebeast.manager;

/**
 * A MinMaxPair is used to describe the min id and max id of a group of Messages.
 * For example, all OrderedMessageBatch objects returned from ADNDatabase have a MinMaxPair.
 */
public class MinMaxPair {
    public String minId;
    public String maxId;

    public MinMaxPair() {}

    /**
     * Construct a new MinMaxPair
     *
     * @param minId the min id
     * @param maxId the max id
     */
    public MinMaxPair(String minId, String maxId) {
        this.minId = minId;
        this.maxId = maxId;
    }

    /**
     * Create a new MinMaxPair using this MinMaxPair's values and another's. The resulting MinMaxPair
     * will have the minimum of the min ids and the maximum of the max ids.
     *
     * @param otherMinMaxPair the other MinMaxPair to combine with this one.
     * @return a new MinMaxPair using this MinMaxPair's values and another's
     */
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

    /**
     * @return the Integer value of the max id.
     */
    public Integer getMaxAsInteger() {
        if(maxId != null) {
            return Integer.parseInt(maxId);
        }
        return null;
    }

    /**
     * @return the Integer value of the min id.
     */
    public Integer getMinAsInteger() {
        if(minId != null) {
            return Integer.parseInt(minId);
        }
        return null;
    }
}
