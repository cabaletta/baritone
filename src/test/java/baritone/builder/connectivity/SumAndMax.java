/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * This file was originally written by btrekkie under the MIT license, which is compatible with the LGPL license for this usage within Baritone
 * https://github.com/btrekkie/dynamic-connectivity/
 */

package baritone.builder.connectivity;

import baritone.builder.utils.com.github.btrekkie.connectivity.Augmentation;
import baritone.builder.utils.com.github.btrekkie.connectivity.MutatingAugmentation;

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

    /**
     * A MutatingAugmentation that combines two SumAndMaxes into one.
     */
    public static final MutatingAugmentation MUTATING_AUGMENTATION = new MutatingAugmentation() {
        @Override
        public void combine(Object value1, Object value2, Object result) {
            SumAndMax sumAndMax1 = (SumAndMax) value1;
            SumAndMax sumAndMax2 = (SumAndMax) value2;
            SumAndMax sumAndMaxResult = (SumAndMax) result;
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

    /**
     * Constructs a new SumAndMax with an arbitrary sum and max.
     */
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
        SumAndMax sumAndMax = (SumAndMax) obj;
        return sum == sumAndMax.sum && max == sumAndMax.max;
    }

    @Override
    public int hashCode() {
        return 31 * sum + max;
    }
}
