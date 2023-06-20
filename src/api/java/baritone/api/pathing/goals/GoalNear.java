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
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.doubles.DoubleOpenHashSet;
import net.minecraft.util.math.BlockPos;

public class GoalNear implements Goal, IGoalRenderPos {

    private final int x;
    private final int y;
    private final int z;
    private final int rangeSq;

    public GoalNear(BlockPos pos, int range) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.rangeSq = range * range;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        int xDiff = x - this.x;
        int yDiff = y - this.y;
        int zDiff = z - this.z;
        return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff <= rangeSq;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        int xDiff = x - this.x;
        int yDiff = y - this.y;
        int zDiff = z - this.z;
        return GoalBlock.calculate(xDiff, yDiff, zDiff);
    }

    @Override
    public double heuristic() {// TODO less hacky solution
        int range = (int) Math.ceil(Math.sqrt(rangeSq));
        DoubleOpenHashSet maybeAlwaysInside = new DoubleOpenHashSet(); // see pull request #1978
        double minOutside = Double.POSITIVE_INFINITY;
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    double h = heuristic(x + dx, y + dy, z + dz);
                    if (h < minOutside && isInGoal(x + dx, y + dy, z + dz)) {
                        maybeAlwaysInside.add(h);
                    } else {
                        minOutside = Math.min(minOutside, h);
                    }
                }
            }
        }
        double maxInside = Double.NEGATIVE_INFINITY;
        DoubleIterator it = maybeAlwaysInside.iterator();
        while (it.hasNext()) {
            double inside = it.nextDouble();
            if (inside < minOutside) {
                maxInside = Math.max(maxInside, inside);
            }
        }
        return maxInside;
    }

    @Override
    public BlockPos getGoalPos() {
        return new BlockPos(x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GoalNear goal = (GoalNear) o;
        return x == goal.x
                && y == goal.y
                && z == goal.z
                && rangeSq == goal.rangeSq;
    }

    @Override
    public int hashCode() {
        return (int) BetterBlockPos.longHash(x, y, z) + rangeSq;
    }

    @Override
    public String toString() {
        return String.format(
                "GoalNear{x=%s, y=%s, z=%s, rangeSq=%d}",
                SettingsUtil.maybeCensor(x),
                SettingsUtil.maybeCensor(y),
                SettingsUtil.maybeCensor(z),
                rangeSq
        );
    }
}
