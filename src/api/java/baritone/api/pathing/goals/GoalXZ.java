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
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.SettingsUtil;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

/**
 * Useful for long-range goals that don't have a specific Y level.
 *
 * @author leijurv
 */
public class GoalXZ implements Goal {

    private static final double SQRT_2 = Math.sqrt(2);

    /**
     * The X block position of this goal
     */
    private final int x;

    /**
     * The Z block position of this goal
     */
    private final int z;

    public GoalXZ(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public GoalXZ(BetterBlockPos pos) {
        this.x = pos.x;
        this.z = pos.z;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        return x == this.x && z == this.z;
    }

    @Override
    public double heuristic(int x, int y, int z) {//mostly copied from GoalBlock
        int xDiff = x - this.x;
        int zDiff = z - this.z;
        return calculate(xDiff, zDiff);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GoalXZ goal = (GoalXZ) o;
        return x == goal.x && z == goal.z;
    }

    @Override
    public int hashCode() {
        int hash = 1791873246;
        hash = hash * 222601791 + x;
        hash = hash * -1331679453 + z;
        return hash;
    }

    @Override
    public String toString() {
        return String.format(
                "GoalXZ{x=%s,z=%s}",
                SettingsUtil.maybeCensor(x),
                SettingsUtil.maybeCensor(z)
        );
    }

    public static double calculate(double xDiff, double zDiff) {
        //This is a combination of pythagorean and manhattan distance
        //It takes into account the fact that pathing can either walk diagonally or forwards

        //It's not possible to walk forward 1 and right 2 in sqrt(5) time
        //It's really 1+sqrt(2) because it'll walk forward 1 then diagonally 1
        double x = Math.abs(xDiff);
        double z = Math.abs(zDiff);
        double straight;
        double diagonal;
        if (x < z) {
            straight = z - x;
            diagonal = x;
        } else {
            straight = x - z;
            diagonal = z;
        }
        diagonal *= SQRT_2;
        return (diagonal + straight) * BaritoneAPI.getSettings().costHeuristic.value; // big TODO tune
    }

    public static GoalXZ fromDirection(Vector3d origin, float yaw, double distance) {
        float theta = (float) Math.toRadians(yaw);
        double x = origin.x - MathHelper.sin(theta) * distance;
        double z = origin.z + MathHelper.cos(theta) * distance;
        return new GoalXZ(MathHelper.floor(x), MathHelper.floor(z));
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }
}
