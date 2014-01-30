package com.alwaysallthetime.messagebeast.manager;

import java.util.Comparator;

public class ReverseChronologicalComparator implements Comparator<Long> {
    @Override
    public int compare(Long lhs, Long rhs) {
        return rhs.compareTo(lhs);
    }
}
