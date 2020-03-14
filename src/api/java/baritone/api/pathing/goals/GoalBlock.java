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

import baritone.api.utils.SettingsUtil;
import baritone.api.utils.interfaces.IGoalRenderPos;
import net.minecraft.util.math.BlockPos;

/**
 * A specific BlockPos goal
 *
 * @author leijurv
 */
public class GoalBlock implements Goal, IGoalRenderPos {

    /**
     * The X block position of this goal
     */
    public final int x;

    /**
     * The Y block position of this goal
     */
    public final int y;

    /**
     * The Z block position of this goal
     */
    public final int z;

    public GoalBlock(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public GoalBlock(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        return x == this.x && y == this.y && z == this.z;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        int xDiff = x - this.x;
        int yDiff = y - this.y;
        int zDiff = z - this.z;
        return calculate(xDiff, yDiff, zDiff);
    }

    @Override
    public String toString() {
        return String.format(
                "GoalBlock{x=%s,y=%s,z=%s}",
                SettingsUtil.maybeCensor(x),
                SettingsUtil.maybeCensor(y),
                SettingsUtil.maybeCensor(z)
        );
    }

    /**
     * @return The position of this goal as a {@link BlockPos}
     */
    @Override
    public BlockPos getGoalPos() {
        return new BlockPos(x, y, z);
    }

    public static double calculate(double xDiff, int yDiff, double zDiff) {
        double heuristic = 0;

        // if yDiff is 1 that means that pos.getY()-this.y==1 which means that we're 1 block below where we should be
        // therefore going from 0,0,0 to a GoalYLevel of pos.getY()-this.y is accurate
        heuristic += GoalYLevel.calculate(yDiff, 0);

        //use the pythagorean and manhattan mixture from GoalXZ
        heuristic += GoalXZ.calculate(xDiff, zDiff);
        return heuristic;
    }
}
