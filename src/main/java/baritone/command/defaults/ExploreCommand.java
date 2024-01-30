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
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.datatypes.RelativeGoalXZ;
import baritone.api.command.exception.CommandException;
import baritone.api.pathing.goals.GoalXZ;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class ExploreCommand extends Command {

    public ExploreCommand(IBaritone baritone) {
        super(baritone, "explore");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        if (args.hasAny()) {
            args.requireExactly(2);
        } else {
            args.requireMax(0);
        }
        GoalXZ goal = args.hasAny()
                ? args.getDatatypePost(RelativeGoalXZ.INSTANCE, ctx.playerToes())
                : new GoalXZ(ctx.playerToes());
        baritone.getExploreProcess().explore(goal.getX(), goal.getZ());
        logDirect(String.format("Exploring from %s", goal.toString()));
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        if (args.hasAtMost(2)) {
            return args.tabCompleteDatatype(RelativeGoalXZ.INSTANCE);
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Explore things";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Tell Baritone to explore randomly. If you used explorefilter before this, it will be applied.",
                "",
                "Usage:",
                "> explore - Explore from your current position.",
                "> explore <x> <z> - Explore from the specified X and Z position."
        );
    }
}
