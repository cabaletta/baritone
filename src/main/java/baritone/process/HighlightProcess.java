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

package baritone.process;

import baritone.Baritone;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalComposite;
import baritone.api.process.IHighlightProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.pathing.movement.CalculationContext;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Highlight blocks of a certain type
 *
 * @author stackmagic
 * @since 2022-03-22
 */
public class HighlightProcess extends MineProcess implements IHighlightProcess {

    public HighlightProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public void highlight(BlockOptionalMetaLookup filter) {
        mine(filter);
    }

    @Override
    public String displayName0() {
        return "Highlight " + filter;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        int mineGoalUpdateInterval = Baritone.settings().mineGoalUpdateInterval.value;
        List<BlockPos> curr = new ArrayList<>(knownOreLocations);
        if (mineGoalUpdateInterval != 0 && tickCount++ % mineGoalUpdateInterval == 0) { // big brain
            CalculationContext context = new CalculationContext(baritone, true);
            Baritone.getExecutor().execute(() -> rescan(curr, context));
        }
        if (Baritone.settings().legitMine.value) {
            addNearby();
        }

        return updateGoal();
    }

    @Override
    protected PathingCommand updateGoal() {
        List<BlockPos> locs = knownOreLocations;
        if (!locs.isEmpty()) {
            CalculationContext context = new CalculationContext(baritone);
            List<BlockPos> locs2 = prune(context, new ArrayList<>(locs), filter, ORE_LOCATIONS_COUNT, List.of(), droppedItemsScan());
            // can't reassign locs, gotta make a new var locs2, because we use it in a lambda right here, and variables you use in a lambda must be effectively final
            Goal goal = new GoalComposite(locs2.stream().map(loc -> coalesce(loc, locs2, context)).toArray(Goal[]::new));
            knownOreLocations = locs2;
            return new PathingCommand(goal, PathingCommandType.CANCEL_AND_SET_GOAL);
        }

        return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
    }
}
