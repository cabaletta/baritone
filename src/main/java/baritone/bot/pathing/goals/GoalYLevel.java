/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot.pathing.goals;

import net.minecraft.util.math.BlockPos;

/**
 * Useful for mining (getting to diamond / iron level)
 * @author leijurv
 */
public class GoalYLevel implements Goal {

    final int level;

    public GoalYLevel(int level) {
        this.level = level;
    }

    @Override
    public boolean isInGoal(BlockPos pos) {
        return pos.getY() == level;
    }

    @Override
    public double heuristic(BlockPos pos) {
        return 20 * Math.abs(pos.getY() - level);//the number 20 was chosen somewhat randomly.
        //TODO fix that
    }

    @Override
    public String toString() {
        return "Goal{y=" + level + "}";
    }
}
