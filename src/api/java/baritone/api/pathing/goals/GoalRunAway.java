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

import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Optional;

/**
 * Useful for automated combat (retreating specifically)
 *
 * @author leijurv
 */
public class GoalRunAway implements Goal {

    private final BlockPos[] from;

    private final double distanceSq;

    private final Optional<Integer> maintainY;

    public GoalRunAway(double distance, BlockPos... from) {
        this(distance, Optional.empty(), from);
    }

    public GoalRunAway(double distance, Optional<Integer> maintainY, BlockPos... from) {
        if (from.length == 0) {
            throw new IllegalArgumentException();
        }
        this.from = from;
        this.distanceSq = distance * distance;
        this.maintainY = maintainY;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        if (maintainY.isPresent() && maintainY.get() != y) {
            return false;
        }
        for (BlockPos p : from) {
            int diffX = x - p.getX();
            int diffZ = z - p.getZ();
            double distSq = diffX * diffX + diffZ * diffZ;
            if (distSq < distanceSq) {
                return false;
            }
        }
        return true;
    }

    @Override
    public double heuristic(int x, int y, int z) {//mostly copied from GoalBlock
        double min = Double.MAX_VALUE;
        for (BlockPos p : from) {
            double h = GoalXZ.calculate(p.getX() - x, p.getZ() - z);
            if (h < min) {
                min = h;
            }
        }
        min = -min;
        if (maintainY.isPresent()) {
            min = min * 0.6 + GoalYLevel.calculate(maintainY.get(), y);
        }
        return min;
    }

    @Override
    public String toString() {
        if (maintainY.isPresent()) {
            return "GoalRunAwayFromMaintainY y=" + maintainY.get() + ", " + Arrays.asList(from);
        } else {
            return "GoalRunAwayFrom" + Arrays.asList(from);
        }
    }
}
