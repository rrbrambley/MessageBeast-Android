package com.alwaysallthetime.messagebeast.manager;

/**
 * A MinMaxDatePair is used to hold the min and max times in millis for a group of Messages.
 * For example, all OrderedMessageBatch objects returned from ADNDatabase have a MinMaxPair.
 */
public class MinMaxDatePair {
    public Long minDate;
    public Long maxDate;

    public MinMaxDatePair() {}

    /**
     * Construct a new MinMaxPair
     *
     * @param minDate the min date as a Long
     * @param maxDate the max date as a Long
     */
    public MinMaxDatePair(Long minDate, Long maxDate) {
        this.minDate = minDate;
        this.maxDate = maxDate;
    }

    /**
     * Update this MinMaxDatePair using this MinMaxDatePair's values and another's. Afterwards, this
     * pair will have the minimum of the min times and the maximum of the max times.
     *
     * @param otherMinMaxPair the other MinMaxDatePair to updateWithCombinedValues with this one.
     */
    public void updateWithCombinedValues(MinMaxDatePair otherMinMaxPair) {
        Long newMin = null, newMax = null;

        if(minDate != null && otherMinMaxPair.minDate != null) {
            newMin = Math.min(minDate, otherMinMaxPair.minDate);
        } else if(otherMinMaxPair.minDate != null) {
            newMin = otherMinMaxPair.minDate;
        } else if(minDate != null) {
            newMin = minDate;
        }

        if(maxDate != null && otherMinMaxPair.maxDate != null) {
            newMax = Math.max(maxDate, otherMinMaxPair.maxDate);
        } else if(otherMinMaxPair.maxDate != null) {
            newMax = otherMinMaxPair.maxDate;
        } else if(maxDate != null) {
            newMax = maxDate;
        }

        minDate = newMin;
        maxDate = newMax;
    }

    /**
     * If the provided time in millis is greater than the max of this object, or less than the
     * min of this object, then alter this MinMaxDatePair's values accordingly.
     *
     * @param newDate
     */
    public void expandIfMinOrMax(Long newDate) {
        minDate = minDate == null ? newDate : Math.min(minDate, newDate);
        maxDate = maxDate == null ? newDate : Math.max(maxDate, newDate);
    }
}
