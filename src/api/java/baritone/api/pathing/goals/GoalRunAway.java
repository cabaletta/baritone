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
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;

/**
 * Useful for automated combat (retreating specifically)
 *
 * @author leijurv
 */
public class GoalRunAway implements Goal {

    private final BlockPos[] from;

    private final double distanceSq;

    private final Integer maintainY;

    public GoalRunAway(double distance, BlockPos... from) {
        this(distance, null, from);
    }

    public GoalRunAway(double distance, Integer maintainY, BlockPos... from) {
        if (from.length == 0) {
            throw new IllegalArgumentException();
        }
        this.from = from;
        this.distanceSq = distance * distance;
        this.maintainY = maintainY;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        if (maintainY != null && maintainY != y) {
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
        if (maintainY != null) {
            min = min * 0.6 + GoalYLevel.calculate(maintainY, y) * 1.5;
        }
        return min;
    }

    @Override
    public double heuristic() {//TODO less hacky solution
        int distance = (int)Math.ceil(Math.abs(Math.sqrt(distanceSq)));
        int minX = from[0].getX() - distance;
        int minY = from[0].getY() - distance;
        int minZ = from[0].getZ() - distance;
        int maxX = from[0].getX() + distance;
        int maxY = from[0].getY() + distance;
        int maxZ = from[0].getZ() + distance;
        for (BlockPos p : from) {
            minX = Math.min(minX, p.getX() - distance);
            minY = Math.min(minY, p.getY() - distance);
            minZ = Math.min(minZ, p.getZ() - distance);
            maxX = Math.max(minX, p.getX() + distance);
            maxY = Math.max(minY, p.getY() + distance);
            maxZ = Math.max(minZ, p.getZ() + distance);
        }
        HashSet<Double> maybeAlwaysInside = new HashSet<>();
        HashSet<Double> sometimesOutside = new HashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    double h = heuristic(x, y, z);
                    if (!sometimesOutside.contains(h) && isInGoal(x, y, z)) {
                        maybeAlwaysInside.add(h);
                    } else {
                        maybeAlwaysInside.remove(h);
                        sometimesOutside.add(h);
                    }
                }
            }
        }
        try {
            return Collections.max(maybeAlwaysInside);
        } catch (NoSuchElementException e) {
            return Double.NEGATIVE_INFINITY;
        }
    }

    @Override
    public String toString() {
        if (maintainY != null) {
            return String.format(
                    "GoalRunAwayFromMaintainY y=%s, %s",
                    SettingsUtil.maybeCensor(maintainY),
                    Arrays.asList(from)
            );
        } else {
            return "GoalRunAwayFrom" + Arrays.asList(from);
        }
    }
}
