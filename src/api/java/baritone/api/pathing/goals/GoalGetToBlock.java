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

import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.SettingsUtil;
import baritone.api.utils.interfaces.IGoalRenderPos;
import net.minecraft.core.BlockPos;


/**
 * Don't get into the block, but get directly adjacent to it. Useful for chests.
 *
 * @author avecowa
 */
public class GoalGetToBlock implements Goal, IGoalRenderPos {

    public final int x;
    public final int y;
    public final int z;

    public GoalGetToBlock(BlockPos pos) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
    }

    @Override
    public BlockPos getGoalPos() {
        return new BlockPos(x, y, z);
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        int xDiff = x - this.x;
        int yDiff = y - this.y;
        int zDiff = z - this.z;
        return Math.abs(xDiff) + Math.abs(yDiff < 0 ? yDiff + 1 : yDiff) + Math.abs(zDiff) <= 1;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        int xDiff = x - this.x;
        int yDiff = y - this.y;
        int zDiff = z - this.z;
        return GoalBlock.calculate(xDiff, yDiff < 0 ? yDiff + 1 : yDiff, zDiff);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GoalGetToBlock goal = (GoalGetToBlock) o;
        return x == goal.x
                && y == goal.y
                && z == goal.z;
    }

    @Override
    public int hashCode() {
        return (int) BetterBlockPos.longHash(x, y, z) * -49639096;
    }

    @Override
    public String toString() {
        return String.format(
                "GoalGetToBlock{x=%s,y=%s,z=%s}",
                SettingsUtil.maybeCensor(x),
                SettingsUtil.maybeCensor(y),
                SettingsUtil.maybeCensor(z)
        );
    }
}
