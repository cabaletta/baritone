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

package baritone.command.defaults;

import baritone.api.IBaritone;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalStrictDirection;
import baritone.api.command.Command;
import baritone.api.command.exception.CommandException;
import baritone.api.command.argument.IArgConsumer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class TunnelCommand extends Command {

    public TunnelCommand(IBaritone baritone) {
        super(baritone, "tunnel");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(0);
        Goal goal = new GoalStrictDirection(
                ctx.playerFeet(),
                ctx.player().getHorizontalFacing()
        );
        baritone.getCustomGoalProcess().setGoalAndPath(goal);
        logDirect(String.format("Goal: %s", goal.toString()));
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Set a goal to tunnel in your current direction";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "The tunnel command sets a goal that tells Baritone to mine completely straight in the direction that you're facing.",
                "",
                "Usage:",
                "> tunnel"
        );
    }
}
