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
import net.minecraft.util.math.BlockPos;


/**
 * Don't get into the block, but get directly adjacent to it. Useful for chests.
 *
 * @author avecowa
 */
public class GoalGetToBlock implements Goal, IGoalRenderPos {

    private final int x;
    private final int y;
    private final int z;

    public GoalGetToBlock(BlockPos pos) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
    }

    public BlockPos getGoalPos() {
        return new BlockPos(x, y, z);
    }

    @Override
    public boolean isInGoal(BlockPos pos) {
        int xDiff = pos.getX() - this.x;
        int yDiff = pos.getY() - this.y;
        int zDiff = pos.getZ() - this.z;
        if (yDiff < 0) {
            yDiff++;
        }
        return Math.abs(xDiff) + Math.abs(yDiff) + Math.abs(zDiff) <= 1;
    }

    @Override
    public double heuristic(BlockPos pos) {
        int xDiff = pos.getX() - this.x;
        int yDiff = pos.getY() - this.y;
        int zDiff = pos.getZ() - this.z;
        return GoalBlock.calculate(xDiff, yDiff, zDiff);
    }
}
