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

package baritone.api.pathing.goals;

import baritone.api.BaritoneAPI;

public class GoalAxis implements Goal {

    private static final double SQRT_2_OVER_2 = Math.sqrt(2) / 2;

    @Override
    public boolean isInGoal(int x, int y, int z) {
        return y == BaritoneAPI.getSettings().axisHeight.value && (x == 0 || z == 0 || Math.abs(x) == Math.abs(z));
    }

    @Override
    public double heuristic(int x0, int y, int z0) {
        int x = Math.abs(x0);
        int z = Math.abs(z0);

        int major = Math.min(x, z);
        int minor = Math.abs(x - z);

        double flatAxisDistance = Math.min(major, minor * SQRT_2_OVER_2);

        return flatAxisDistance * BaritoneAPI.getSettings().costHeuristic.value + GoalYLevel.calculate(BaritoneAPI.getSettings().axisHeight.value, y);
    }

    @Override
    public boolean equals(Object o) {
        return o.getClass() == GoalAxis.class;
    }

    @Override
    public int hashCode() {
        return 201385781;
    }

    @Override
    public String toString() {
        return "GoalAxis";
    }
}
