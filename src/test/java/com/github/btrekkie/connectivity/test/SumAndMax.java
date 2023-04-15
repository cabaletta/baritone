package com.github.btrekkie.connectivity.test;

import com.github.btrekkie.connectivity.Augmentation;
import com.github.btrekkie.connectivity.MutatingAugmentation;

/** Stores two values: a sum and a maximum. Used for testing augmentation in ConnGraph. */
class SumAndMax {
    /** An Augmentation that combines two SumAndMaxes into one. */
    public static final Augmentation AUGMENTATION = new Augmentation() {
        @Override
        public Object combine(Object value1, Object value2) {
            SumAndMax sumAndMax1 = (SumAndMax)value1;
            SumAndMax sumAndMax2 = (SumAndMax)value2;
            return new SumAndMax(sumAndMax1.sum + sumAndMax2.sum, Math.max(sumAndMax1.max, sumAndMax2.max));
        }
    };

    /** A MutatingAugmentation that combines two SumAndMaxes into one. */
    public static final MutatingAugmentation MUTATING_AUGMENTATION = new MutatingAugmentation() {
        @Override
        public void combine(Object value1, Object value2, Object result) {
            SumAndMax sumAndMax1 = (SumAndMax)value1;
            SumAndMax sumAndMax2 = (SumAndMax)value2;
            SumAndMax sumAndMaxResult = (SumAndMax)result;
            sumAndMaxResult.sum = sumAndMax1.sum + sumAndMax2.sum;
            sumAndMaxResult.max = Math.max(sumAndMax1.max, sumAndMax2.max);
        }

        @Override
        public Object newAugmentation() {
            return new SumAndMax();
        }
    };

    public int sum;

    public int max;

    /** Constructs a new SumAndMax with an arbitrary sum and max. */
    public SumAndMax() {

    }

    public SumAndMax(int sum, int max) {
        this.sum = sum;
        this.max = max;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SumAndMax)) {
            return false;
        }
        SumAndMax sumAndMax = (SumAndMax)obj;
        return sum == sumAndMax.sum && max == sumAndMax.max;
    }

    @Override
    public int hashCode() {
        return 31 * sum + max;
    }
}
