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

package baritone.pathing.goals;

import baritone.pathing.movement.ActionCosts;
import net.minecraft.util.math.BlockPos;

/**
 * An abstract Goal for pathing, can be anything from a specific block to just a Y coordinate.
 *
 * @author leijurv
 */
public interface Goal extends ActionCosts {

    /**
     * Returns whether or not the specified position
     * meets the requirement for this goal based.
     *
     * @param pos The position
     * @return Whether or not it satisfies this goal
     */
    boolean isInGoal(BlockPos pos);

    /**
     * Estimate the number of ticks it will take to get to the goal
     *
     * @param pos The
     * @return The estimate number of ticks to satisfy the goal
     */
    double heuristic(BlockPos pos);
}
