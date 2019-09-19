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

package baritone.utils.command.defaults;

import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.command.Command;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class ThisWayCommand extends Command {
    public ThisWayCommand(IBaritone baritone) {
        super(baritone, asList("thisway", "forward"));
    }

    @Override
    protected void executed(String label, ArgConsumer args, Settings settings) {
        args.requireExactly(1);

        GoalXZ goal = GoalXZ.fromDirection(
                ctx.playerFeetAsVec(),
                ctx.player().rotationYawHead,
                args.getAs(Double.class)
        );

        baritone.getCustomGoalProcess().setGoal(goal);
        logDirect(String.format("Goal: %s", goal));
    }

    @Override
    protected Stream<String> tabCompleted(String label, ArgConsumer args, Settings settings) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Travel in your current direction";
    }

    @Override
    public List<String> getLongDesc() {
        return asList(
                "Creates a GoalXZ some amount of blocks in the direction you're currently looking",
                "",
                "Usage:",
                "> thisway <distance> - makes a GoalXZ distance blocks in front of you"
        );
    }
}
