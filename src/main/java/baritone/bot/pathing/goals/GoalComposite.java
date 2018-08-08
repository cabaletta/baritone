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

import java.util.Arrays;
import java.util.Collection;
import net.minecraft.util.math.BlockPos;

/**
 * A composite of many goals, any one of which satisfies the composite.
 * For example, a GoalComposite of block goals for every oak log in loaded chunks
 * would result in it pathing to the easiest oak log to get to
 * @author avecowa
 */
public class GoalComposite implements Goal {

    public final Goal[] goals;

    public GoalComposite(Goal... goals) {
        this.goals = goals;
    }

    public GoalComposite(BlockPos... blocks) {
        this(Arrays.asList(blocks));
    }

    public GoalComposite(Collection<BlockPos> blocks) {
        this(blocks.stream().map(GoalBlock::new).toArray(Goal[]::new));
    }

    @Override
    public boolean isInGoal(BlockPos pos) {
        return Arrays.stream(this.goals).anyMatch(goal -> goal.isInGoal(pos));
    }

    @Override
    public double heuristic(BlockPos pos) {
        double min = Double.MAX_VALUE;
        for (Goal g : goals) {
            min = Math.min(min, g.heuristic(pos)); // whichever is closest
        }
        return min;
    }

    @Override
    public String toString() {
        return "GoalComposite" + Arrays.toString(goals);
    }

    public Goal[] goals() {
        return goals;
    }
}
