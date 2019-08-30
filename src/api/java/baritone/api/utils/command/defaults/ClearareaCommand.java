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

package baritone.api.utils.command.defaults;

import baritone.api.Settings;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.command.Command;
import baritone.api.utils.command.datatypes.RelativeBlockPos;
import baritone.api.utils.command.exception.CommandInvalidStateException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class ClearareaCommand extends Command {
    public ClearareaCommand() {
        super("cleararea", "Clear an area of all blocks");
    }

    @Override
    protected void executed(String label, ArgConsumer args, Settings settings) {
        BetterBlockPos pos1 = ctx.playerFeet();
        BetterBlockPos pos2;

        if (args.has()) {
            args.requireMax(3);
            pos2 = args.getDatatype(RelativeBlockPos.class).apply(pos1);
        } else {
            args.requireMax(0);

            Goal goal = baritone.getCustomGoalProcess().getGoal();

            if (!(goal instanceof GoalBlock)) {
                throw new CommandInvalidStateException("Goal is not a GoalBlock");
            } else {
                pos2 = new BetterBlockPos(((GoalBlock) goal).getGoalPos());
            }
        }

        baritone.getBuilderProcess().clearArea(pos1, pos2);
        logDirect("Success");
    }

    @Override
    protected Stream<String> tabCompleted(String label, ArgConsumer args, Settings settings) {
        return args.tabCompleteDatatype(RelativeBlockPos.class);
    }

    @Override
    public List<String> getLongDesc() {
        return asList(
            "Clear an area of all blocks.",
            "",
            "Usage:",
            "> cleararea - Clears the area marked by your current position and the current GoalBlock",
            "> cleararea <x> <y> <z> - Custom second corner rather than your goal"
        );
    }
}
