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

package baritone.utils;

/**
 * @author Brady
 */
public final class BaritoneMath {

    private static final double FLOOR_DOUBLE_D = 1_073_741_824.0;
    private static final int FLOOR_DOUBLE_I = 1_073_741_824;

    private BaritoneMath() {}

    public static int fastFloor(final double v) {
        return (int) (v + FLOOR_DOUBLE_D) - FLOOR_DOUBLE_I;
    }

    public static int fastCeil(final double v) {
        return FLOOR_DOUBLE_I - (int) (FLOOR_DOUBLE_D - v);
    }

    public static double normalize(double value, double maxValue) {
        if (maxValue == 0) return 0;
        return value/maxValue;
    }
}
