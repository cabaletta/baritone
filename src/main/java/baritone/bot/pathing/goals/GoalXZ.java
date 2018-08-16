/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot.pathing.goals;

import baritone.bot.Baritone;
import baritone.bot.utils.Utils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

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

    @Override
    public boolean isInGoal(BlockPos pos) {
        return pos.getX() == x && pos.getZ() == z;
    }

    @Override
    public double heuristic(BlockPos pos) {//mostly copied from GoalBlock
        double xDiff = pos.getX() - this.x;
        double zDiff = pos.getZ() - this.z;
        return calculate(xDiff, zDiff);
    }

    @Override
    public String toString() {
        return "Goal{x=" + x + ",z=" + z + "}";
    }

    public static double calculate(double xDiff, double zDiff) {
        return calculate(xDiff, zDiff, 0);
    }

    /*
     public static double calculate(double xDiff, double zDiff) {
     double pythaDist = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
     return calculate(xDiff, zDiff, pythaDist);
     }
     public static double calculateOld(double xDiff, double zDiff, double pythaDist) {
     double heuristic = 0;
     heuristic += Math.abs(xDiff) * Movement.WALK_ONE_BLOCK_COST * 1.1;//overestimate
     heuristic += Math.abs(zDiff) * Movement.WALK_ONE_BLOCK_COST * 1.1;
     heuristic += pythaDist / 10 * Movement.WALK_ONE_BLOCK_COST;
     return heuristic;
     }
     */

    public static double calculate(double xDiff, double zDiff, double pythaDist) {
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
        return (diagonal + straight) * Baritone.settings().costHeuristic.get(); // big TODO tune
    }

    public static GoalXZ fromDirection(Vec3d origin, float yaw, double distance) {
        double theta = Utils.degToRad(yaw);
        double x = origin.x - Math.sin(theta) * distance;
        double z = origin.z + Math.cos(theta) * distance;
        return new GoalXZ((int) x, (int) z);
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }
}
