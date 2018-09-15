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

import baritone.Baritone;
import net.minecraft.util.math.BlockPos;

public class GoalAxis implements Goal {

    private static final double SQRT_2_OVER_2 = Math.sqrt(2) / 2;

    @Override
    public boolean isInGoal(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        return y == Baritone.settings().axisHeight.get() && (x == 0 || z == 0 || Math.abs(x) == Math.abs(z));
    }

    @Override
    public double heuristic(BlockPos pos) {
        int x = Math.abs(pos.getX());
        int y = pos.getY();
        int z = Math.abs(pos.getZ());

        int shrt = Math.min(x, z);
        int lng = Math.max(x, z);
        int diff = lng - shrt;

        double flatAxisDistance = Math.min(x, Math.min(z, diff * SQRT_2_OVER_2));

        return flatAxisDistance * Baritone.settings().costHeuristic.get() + GoalYLevel.calculate(Baritone.settings().axisHeight.get(), y);
    }

    @Override
    public String toString() {
        return "GoalAxis";
    }
}
