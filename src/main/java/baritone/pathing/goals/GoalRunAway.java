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

import java.util.Arrays;

/**
 * Useful for automated combat (retreating specifically)
 *
 * @author leijurv
 */
public class GoalRunAway implements Goal {

    private final BlockPos[] from;

    private final double distanceSq;

    public GoalRunAway(double distance, BlockPos... from) {
        if (from.length == 0) {
            throw new IllegalArgumentException();
        }
        this.from = from;
        this.distanceSq = distance * distance;
    }

    @Override
    public boolean isInGoal(BlockPos pos) {
        for (BlockPos p : from) {
            int diffX = pos.getX() - p.getX();
            int diffZ = pos.getZ() - p.getZ();
            double distSq = diffX * diffX + diffZ * diffZ;
            if (distSq < distanceSq) {
                return false;
            }
        }
        return true;
    }

    @Override
    public double heuristic(BlockPos pos) {//mostly copied from GoalBlock
        double min = Double.MAX_VALUE;
        for (BlockPos p : from) {
            double h = GoalXZ.calculate(p.getX() - pos.getX(), p.getZ() - pos.getZ());
            if (h < min) {
                min = h;
            }
        }
        return -min;
    }

    @Override
    public String toString() {
        return "GoalRunAwayFrom" + Arrays.asList(from);
    }
}
