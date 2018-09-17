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

package baritone.pathing.goals;

import baritone.utils.interfaces.IGoalRenderPos;
import net.minecraft.util.math.BlockPos;

/**
 * Useful if the goal is just to mine a block. This goal will be satisfied if the specified
 * {@link BlockPos} is at to or above the specified position for this goal.
 *
 * @author leijurv
 */
public class GoalTwoBlocks implements Goal, IGoalRenderPos {

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

    public GoalTwoBlocks(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public GoalTwoBlocks(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean isInGoal(BlockPos pos) {
        return pos.getX() == this.x && (pos.getY() == this.y || pos.getY() == this.y - 1) && pos.getZ() == this.z;
    }

    @Override
    public double heuristic(BlockPos pos) {
        double xDiff = pos.getX() - this.x;
        int yDiff = pos.getY() - this.y;
        if (yDiff < 0) {
            yDiff++;
        }
        double zDiff = pos.getZ() - this.z;
        return GoalBlock.calculate(xDiff, yDiff, zDiff);
    }

    @Override
    public BlockPos getGoalPos() {
        return new BlockPos(x, y, z);
    }

    @Override
    public String toString() {
        return "GoalTwoBlocks{x=" + x + ",y=" + y + ",z=" + z + "}";
    }
}
