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

import net.minecraft.util.math.BlockPos;

/**
 * A specific BlockPos goal
 *
 * @author leijurv
 */
public class GoalBlock implements Goal {

    /**
     * The X block position of this goal
     */
    private final int x;

    /**
     * The Y block position of this goal
     */
    private final int y;

    /**
     * The Z block position of this goal
     */
    private final int z;

    public GoalBlock(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public GoalBlock(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean isInGoal(BlockPos pos) {
        return pos.getX() == this.x && pos.getY() == this.y && pos.getZ() == this.z;
    }

    /**
     * The min range value over which to begin considering Y coordinate in the heuristic
     */
    private static final double MIN = 20;

    /**
     * The max range value over which to begin considering Y coordinate in the heuristic
     */
    private static final double MAX = 150;

    @Override
    public double heuristic(BlockPos pos) {
        double xDiff = pos.getX() - this.x;
        double yDiff = pos.getY() - this.y;
        double zDiff = pos.getZ() - this.z;
        return calculate(xDiff, yDiff, zDiff);
    }

    @Override
    public String toString() {
        return "Goal{x=" + x + ",y=" + y + ",z=" + z + "}";
    }

    /**
     * @return The position of this goal as a {@link BlockPos}
     */
    public BlockPos getGoalPos() {
        return new BlockPos(x, y, z);
    }

    public static double calculate(double xDiff, double yDiff, double zDiff) {
        double pythaDist = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
        double heuristic = 0;
        double baseline = (PLACE_ONE_BLOCK_COST + FALL_N_BLOCKS_COST[1]) * 32;
        if (pythaDist < MAX) {//if we are more than MAX away, ignore the Y coordinate. It really doesn't matter how far away your Y coordinate is if you X coordinate is 1000 blocks away.
            //as we get closer, slowly reintroduce the Y coordinate as a heuristic cost
            double multiplier = pythaDist < MIN ? 1 : 1 - (pythaDist - MIN) / (MAX - MIN);
            if (yDiff < 0) {//pos.getY()-this.y<0 therefore pos.getY()<this.y, so target is above current
                heuristic -= yDiff * (PLACE_ONE_BLOCK_COST * 0.7 + JUMP_ONE_BLOCK_COST);//target above current
            } else {
                heuristic += yDiff * (10 + FALL_N_BLOCKS_COST[1]);//target below current
            }
            heuristic *= multiplier;
            heuristic += (1 - multiplier) * baseline;
        } else {
            heuristic += baseline;
        }
        //use the pythagorean and manhattan mixture from GoalXZ
        heuristic += GoalXZ.calculate(xDiff, zDiff, pythaDist);
        return heuristic;
    }
}
