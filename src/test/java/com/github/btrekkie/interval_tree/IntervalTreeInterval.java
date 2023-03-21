/*
 * This file was originally written by btrekkie under the MIT license, which is compatible with the LGPL license for this usage within Baritone
 * https://github.com/btrekkie/RedBlackNode/
 */

package com.github.btrekkie.interval_tree;

/**
 * An inclusive range of values [start, end].  Two intervals are equal if they have the same starting and ending values.
 */
public class IntervalTreeInterval {
    /**
     * The smallest value in the range.
     */
    public final double start;

    /**
     * The largest value in the range.
     */
    public final double end;

    public IntervalTreeInterval(double start, double end) {
        if (start > end) {
            throw new IllegalArgumentException("The end of the range must be at most the start");
        }
        this.start = start;
        this.end = end;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof IntervalTreeInterval)) {
            return false;
        }
        IntervalTreeInterval interval = (IntervalTreeInterval) obj;
        return start == interval.start && end == interval.end;
    }
}
