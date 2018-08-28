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

import net.minecraft.util.math.BlockPos;

public class GoalNear implements Goal {
    final int x;
    final int y;
    final int z;
    final int rangeSq;

    public GoalNear(BlockPos pos, int range) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.rangeSq = range * range;
    }

    @Override
    public boolean isInGoal(BlockPos pos) {
        int diffX = x - pos.getX();
        int diffY = y - pos.getY();
        int diffZ = z - pos.getZ();
        return diffX * diffX + diffY * diffY + diffZ * diffZ <= rangeSq;
    }

    @Override
    public double heuristic(BlockPos pos) {
        int diffX = x - pos.getX();
        int diffY = y - pos.getY();
        int diffZ = z - pos.getZ();
        return GoalBlock.calculate(diffX, diffY, diffZ);
    }

    public BlockPos getGoalPos() {
        return new BlockPos(x, y, z);
    }
}
