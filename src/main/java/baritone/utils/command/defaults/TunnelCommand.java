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
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalStrictDirection;
import baritone.api.utils.command.Command;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class TunnelCommand extends Command {
    public TunnelCommand(IBaritone baritone) {
        super(baritone, "tunnel");
    }

    @Override
    protected void executed(String label, ArgConsumer args, Settings settings) {
        args.requireMax(0);

        Goal goal = new GoalStrictDirection(
                ctx.playerFeet(),
                ctx.player().getHorizontalFacing()
        );

        baritone.getCustomGoalProcess().setGoal(goal);
        logDirect(String.format("Goal: %s", goal.toString()));
    }

    @Override
    protected Stream<String> tabCompleted(String label, ArgConsumer args, Settings settings) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Set a goal to tunnel in your current direction";
    }

    @Override
    public List<String> getLongDesc() {
        return asList(
                "The tunnel command sets a goal that tells Baritone to mine completely straight in the direction that you're facing.",
                "",
                "Usage:",
                "> tunnel"
        );
    }
}
