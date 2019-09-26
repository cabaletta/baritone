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
import baritone.api.process.ICustomGoalProcess;
import baritone.api.utils.command.Command;
import baritone.api.utils.command.datatypes.RelativeCoordinate;
import baritone.api.utils.command.datatypes.RelativeGoal;
import baritone.api.utils.command.exception.CommandException;
import baritone.api.utils.command.exception.CommandInvalidStateException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import baritone.api.utils.command.helpers.tabcomplete.TabCompleteHelper;
import baritone.cache.WorldScanner;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class PathCommand extends Command {

    public PathCommand(IBaritone baritone) {
        super(baritone, Arrays.asList("path", "goto"));
    }

    @Override
    protected void executed(String label, ArgConsumer args) throws CommandException {
        ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
        Goal goal;
        if (args.hasAny()) {
            args.requireMax(3);
            goal = args.getDatatypePost(RelativeGoal.INSTANCE, ctx.playerFeet());
        } else if ((goal = customGoalProcess.getGoal()) == null) {
            throw new CommandInvalidStateException("No goal");
        }
        args.requireMax(0);
        WorldScanner.INSTANCE.repack(ctx);
        customGoalProcess.setGoalAndPath(goal);
        logDirect("Now pathing");
    }

    @Override
    protected Stream<String> tabCompleted(String label, ArgConsumer args) throws CommandException {
        if (args.hasAny() && !args.has(4)) {
            while (args.has(2)) {
                if (args.peekDatatypeOrNull(RelativeCoordinate.INSTANCE) == null) {
                    break;
                }
                args.get();
                if (!args.has(2)) {
                    return new TabCompleteHelper()
                            .append("~")
                            .filterPrefix(args.getString())
                            .stream();
                }
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Start heading towards a goal";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "The path command tells Baritone to head towards the current goal.",
                "",
                "Usage:",
                "> path - Start the pathing.",
                "> path <y>",
                "> path <x> <z>",
                "> path <x> <y> <z> - Define the goal here"
        );
    }
}
