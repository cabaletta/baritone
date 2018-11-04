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

package baritone.utils;

import baritone.Baritone;
import baritone.api.pathing.goals.Goal;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import baritone.pathing.path.PathExecutor;
import net.minecraft.util.math.BlockPos;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class PathingControlManager {
    private final Baritone baritone;
    private final HashSet<IBaritoneProcess> processes; // unGh

    public PathingControlManager(Baritone baritone) {
        this.baritone = baritone;
        this.processes = new HashSet<>();
    }

    public void registerProcess(IBaritoneProcess process) {
        processes.add(process);
    }

    public void doTheThingWithTheStuff() {
        PathingCommand cmd = doTheStuff();
        if (cmd == null) {
            baritone.getPathingBehavior().cancel();
            return;
        }

        switch (cmd.commandType) {
            case REQUEST_PAUSE:
                // idk
                // ask pathingbehavior if its safe
            case FORCE_REVALIDATE_GOAL_AND_PATH:
                if (cmd.goal == null) {
                    baritone.getPathingBehavior().cancel(); // todo only if its safe
                    return;
                }
                // pwnage
                baritone.getPathingBehavior().setGoal(cmd.goal);
                if (revalidateGoal(cmd.goal)) {
                    baritone.getPathingBehavior().cancel(); // todo only if its safe
                }
            case REVALIDATE_GOAL_AND_PATH:
                if (cmd.goal == null) {
                    baritone.getPathingBehavior().cancel(); // todo only if its safe
                    return;
                }
                baritone.getPathingBehavior().setGoal(cmd.goal);
                if (Baritone.settings().cancelOnGoalInvalidation.get() && revalidateGoal(cmd.goal)) {
                    baritone.getPathingBehavior().cancel(); // todo only if its safe
                }
            case SET_GOAL_AND_PATH:
                // now this i can do
                if (cmd.goal != null) {
                    baritone.getPathingBehavior().setGoalAndPath(cmd.goal);
                }
                // breaks are for wusses!!!!
        }
    }

    public boolean revalidateGoal(Goal newGoal) {
        PathExecutor current = baritone.getPathingBehavior().getCurrent();
        if (current != null) {
            Goal intended = current.getPath().getGoal();
            BlockPos end = current.getPath().getDest();
            if (intended.isInGoal(end) && !newGoal.isInGoal(end)) {
                // this path used to end in the goal
                // but the goal has changed, so there's no reason to continue...
                return true;
            }
        }
        return false;
    }


    public PathingCommand doTheStuff() {
        List<IBaritoneProcess> inContention = processes.stream().filter(IBaritoneProcess::isActive).sorted(Comparator.comparingDouble(IBaritoneProcess::priority)).collect(Collectors.toList());
        boolean found = false;
        boolean cancelOthers = false;
        PathingCommand exec = null;
        for (int i = inContention.size() - 1; i >= 0; i--) { // truly a gamer moment
            IBaritoneProcess proc = inContention.get(i);
            if (found) {
                if (cancelOthers) {
                    proc.onLostControl();
                }
            } else {
                exec = proc.onTick();
                if (exec == null) {
                    if (proc.isActive()) {
                        throw new IllegalStateException(proc + "");
                    }
                    proc.onLostControl();
                    continue;
                }
                found = true;
                cancelOthers = !proc.isTemporary();
            }
        }
        return exec;
    }
}
