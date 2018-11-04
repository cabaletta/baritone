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
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.process.IGetToBlockProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.List;

public class GetToBlockProcess extends BaritoneProcessHelper implements IGetToBlockProcess {
    Block gettingTo;
    List<BlockPos> knownLocations;

    int tickCount = 0;

    public GetToBlockProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public void getToBlock(Block block) {
        gettingTo = block;
        rescan();
    }

    @Override
    public boolean isActive() {
        return gettingTo != null;
    }

    @Override
    public PathingCommand onTick() {
        if (knownLocations == null) {
            rescan();
        }
        if (knownLocations.isEmpty()) {
            logDirect("No known locations of " + gettingTo);
            onLostControl();
            return null;
        }
        int mineGoalUpdateInterval = Baritone.settings().mineGoalUpdateInterval.get();
        if (mineGoalUpdateInterval != 0 && tickCount++ % mineGoalUpdateInterval == 0) { // big brain
            Baritone.INSTANCE.getExecutor().execute(this::rescan);
        }
        Goal goal = new GoalComposite(knownLocations.stream().map(GoalGetToBlock::new).toArray(Goal[]::new));
        return new PathingCommand(goal, PathingCommandType.SET_GOAL_AND_PATH);
    }

    @Override
    public void onLostControl() {
        gettingTo = null;
        knownLocations = null;
    }

    private void rescan() {
        knownLocations = MineProcess.searchWorld(Collections.singletonList(gettingTo), 64);
    }
}