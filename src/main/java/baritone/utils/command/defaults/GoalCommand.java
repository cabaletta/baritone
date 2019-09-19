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
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.command.Command;
import baritone.api.utils.command.datatypes.RelativeCoordinate;
import baritone.api.utils.command.datatypes.RelativeGoal;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import baritone.api.utils.command.helpers.tabcomplete.TabCompleteHelper;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class GoalCommand extends Command {

    public GoalCommand(IBaritone baritone) {
        super(baritone, "goal");
    }

    @Override
    protected void executed(String label, ArgConsumer args, Settings settings) {
        ICustomGoalProcess goalProcess = baritone.getCustomGoalProcess();

        if (args.has() && asList("reset", "clear", "none").contains(args.peekString())) {
            args.requireMax(1);

            if (nonNull(goalProcess.getGoal())) {
                goalProcess.setGoal(null);
                logDirect("Cleared goal");
            } else {
                logDirect("There was no goal to clear");
            }
        } else {
            args.requireMax(3);
            BetterBlockPos origin = baritone.getPlayerContext().playerFeet();
            Goal goal = args.getDatatype(RelativeGoal.class).apply(origin);
            goalProcess.setGoal(goal);
            logDirect(String.format("Goal: %s", goal.toString()));
        }
    }

    @Override
    protected Stream<String> tabCompleted(String label, ArgConsumer args, Settings settings) {
        TabCompleteHelper helper = new TabCompleteHelper();

        if (args.hasExactlyOne()) {
            helper.append(Stream.of("reset", "clear", "none", "~"));
        } else {
            if (args.hasAtMost(3)) {
                while (args.has(2)) {
                    if (isNull(args.peekDatatypeOrNull(RelativeCoordinate.class))) {
                        break;
                    }

                    args.get();

                    if (!args.has(2)) {
                        helper.append("~");
                    }
                }
            }
        }

        return helper.filterPrefix(args.getString()).stream();
    }

    @Override
    public String getShortDesc() {
        return "Set or clear the goal";
    }

    @Override
    public List<String> getLongDesc() {
        return asList(
                "The goal command allows you to set or clear Baritone's goal.",
                "",
                "Wherever a coordinate is expected, you can use ~ just like in regular Minecraft commands. Or, you can just use regular numbers.",
                "",
                "Usage:",
                "> goal - Set the goal to your current position",
                "> goal <reset/clear/none> - Erase the goal",
                "> goal <y> - Set the goal to a Y level",
                "> goal <x> <z> - Set the goal to an X,Z position",
                "> goal <x> <y> <z> - Set the goal to an X,Y,Z position"
        );
    }
}
