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

import java.util.Collections;
import java.util.HashSet;

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
    public double heuristic() {//TODO less hacky solution
        int range = (int)Math.ceil(Math.abs(Math.sqrt(rangeSq)));
        HashSet<Double> maybeAlwaysInside = new HashSet<>();
        HashSet<Double> sometimesOutside = new HashSet<>();
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    double h = heuristic(x+dx, y+dy, z+dz);
                    if (!sometimesOutside.contains(h) && isInGoal(x+dx, y+dy, z+dz)) {
                        maybeAlwaysInside.add(h);
                    } else {
                        maybeAlwaysInside.remove(h);
                        sometimesOutside.add(h);
                    }
                }
            }
        }
        return Collections.max(maybeAlwaysInside);
    }

    @Override
    public BlockPos getGoalPos() {
        return new BlockPos(x, y, z);
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
