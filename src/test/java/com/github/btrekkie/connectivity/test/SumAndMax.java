package com.github.btrekkie.connectivity.test;

import com.github.btrekkie.connectivity.Augmentation;

/**
 * Stores two values: a sum and a maximum. Used for testing augmentation in ConnGraph.
 */
class SumAndMax {
    /**
     * An Augmentation that combines two SumAndMaxes into one.
     */
    public static final Augmentation AUGMENTATION = new Augmentation() {
        @Override
        public Object combine(Object value1, Object value2) {
            SumAndMax sumAndMax1 = (SumAndMax) value1;
            SumAndMax sumAndMax2 = (SumAndMax) value2;
            return new SumAndMax(sumAndMax1.sum + sumAndMax2.sum, Math.max(sumAndMax1.max, sumAndMax2.max));
        }
    };

    public final int sum;

    public final int max;

    public SumAndMax(int sum, int max) {
        this.sum = sum;
        this.max = max;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SumAndMax)) {
            return false;
        }
        SumAndMax sumAndMax = (SumAndMax) obj;
        return sum == sumAndMax.sum && max == sumAndMax.max;
    }

    @Override
    public int hashCode() {
        return 31 * sum + max;
    }
}
