package com.alwaysallthetime.messagebeast.manager;

/**
 * A MinMaxPair is used to describe the min id and max id of a group of Messages.
 * For example, all OrderedMessageBatch objects returned from ADNDatabase have a MinMaxPair.
 */
public class MinMaxPair {
    public String minId;
    public String maxId;
    public Long minDate;
    public Long maxDate;

    public MinMaxPair() {}

    /**
     * Construct a new MinMaxPair
     *
     * @param minId the min id
     * @param maxId the max id
     * @param minDate the min date in millis
     * @param maxDate the max date in millis
     */
    public MinMaxPair(String minId, String maxId, Long minDate, Long maxDate) {
        this.minId = minId;
        this.maxId = maxId;
        this.minDate = minDate;
        this.maxDate = maxDate;
    }

    /**
     * Combine another MinMaxPair with this one. This one's fields will be altered to contain
     * the mins and maxes of both.
     *
     * @param otherMinMaxPair the other MinMaxPair to combine with this one.
     */
    public void updateWithCombinedValues(MinMaxPair otherMinMaxPair) {
        //
        //id
        //
        Integer thisMin = minId != null ? Integer.parseInt(minId) : null;
        Integer thisMax = maxId != null ? Integer.parseInt(maxId) : null;
        Integer otherMin = otherMinMaxPair.minId != null ? Integer.parseInt(otherMinMaxPair.minId) : null;
        Integer otherMax = otherMinMaxPair.maxId != null ? Integer.parseInt(otherMinMaxPair.maxId) : null;

        String newMinId = null;
        String newMaxId = null;
        if(thisMin != null && otherMin != null) {
            newMinId = String.valueOf(Math.min(thisMin, otherMin));
        } else if(otherMin != null) {
            newMinId = String.valueOf(otherMin);
        } else if(thisMin != null) {
            newMinId = String.valueOf(thisMin);
        }

        if(thisMax != null && otherMax != null) {
            newMaxId = String.valueOf(Math.max(thisMax, otherMax));
        } else if(otherMax != null) {
            newMaxId = String.valueOf(otherMax);
        } else if(thisMax != null) {
            newMaxId = String.valueOf(thisMax);
        }

        this.minId = newMinId;
        this.maxId = newMaxId;

        //
        //date
        //
        Long newMinDate = null, newMaxDate = null;

        if(minDate != null && otherMinMaxPair.minDate != null) {
            newMinDate = Math.min(minDate, otherMinMaxPair.minDate);
        } else if(otherMinMaxPair.minDate != null) {
            newMinDate = otherMinMaxPair.minDate;
        } else if(minDate != null) {
            newMinDate = minDate;
        }

        if(maxDate != null && otherMinMaxPair.maxDate != null) {
            newMaxDate = Math.max(maxDate, otherMinMaxPair.maxDate);
        } else if(otherMinMaxPair.maxDate != null) {
            newMaxDate = otherMinMaxPair.maxDate;
        } else if(maxDate != null) {
            newMaxDate = maxDate;
        }

        this.minDate = newMinDate;
        this.maxDate = newMaxDate;
    }

    /**
     * If the provided date is less than the current min, or greater than the current max,
     * update this MinMaxPair's date accordingly.
     *
     * @param dateInMillis
     */
    public void expandDateIfMinOrMax(Long dateInMillis) {
        minDate = minDate != null ? Math.min(dateInMillis, minDate) : null;
        maxDate = maxDate != null ? Math.min(dateInMillis, maxDate) : null;
    }

    /**
     * @return the Integer value of the max id.
     */
    public Integer getMaxIdAsInteger() {
        if(maxId != null) {
            return Integer.parseInt(maxId);
        }
        return null;
    }

    /**
     * @return the Integer value of the min id.
     */
    public Integer getMinIdAsInteger() {
        if(minId != null) {
            return Integer.parseInt(minId);
        }
        return null;
    }
}
