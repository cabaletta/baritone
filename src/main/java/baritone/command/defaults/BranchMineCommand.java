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
import baritone.api.command.datatypes.RelativeBlockPos;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandInvalidStateException;
import baritone.api.selection.ISelection;
import baritone.api.utils.BetterBlockPos;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class BranchMineCommand extends Command {

    public BranchMineCommand(IBaritone baritone) {
        super(baritone, "branchmine", "bm");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        BetterBlockPos pos1;
        BetterBlockPos pos2;
        if (args.hasExactly(6)) {
            BetterBlockPos origin = ctx.playerFeet();
            pos1 = args.getDatatypePost(RelativeBlockPos.INSTANCE, origin);
            pos2 = args.getDatatypePost(RelativeBlockPos.INSTANCE, origin);
        } else if (args.hasExactly(0)) {
            ISelection[] selections = baritone.getSelectionManager().getSelections();
            if (selections.length == 0) {
                throw new CommandInvalidStateException("No selection. Use #sel to create one, or provide coordinates.");
            }
            if (selections.length > 1) {
                throw new CommandInvalidStateException("Multiple selections found. Use #sel to keep only one, or provide coordinates.");
            }
            ISelection selection = selections[0];
            pos1 = selection.pos1();
            pos2 = selection.pos2();
        } else {
            throw new CommandInvalidStateException("Usage: #branchmine <x1> <y1> <z1> <x2> <y2> <z2> or #branchmine with a selection");
        }
        baritone.getBranchMineProcess().branchMine(pos1, pos2);
        logDirect("Starting branch mine from " + pos1 + " to " + pos2);
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasAtMost(3)) {
            return args.tabCompleteDatatype(RelativeBlockPos.INSTANCE);
        } else if (args.hasAtMost(6)) {
            return args.tabCompleteDatatype(RelativeBlockPos.INSTANCE);
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Branch mine an area";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Mines an area using an optimized branch-mine pattern.",
                "",
                "Usage:",
                "> branchmine <x1> <y1> <z1> <x2> <y2> <z2> - Mine the specified area.",
                "> branchmine - Mine the current selection."
        );
    }
}
