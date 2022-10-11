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
import baritone.api.command.exception.CommandException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LitematicaCommand extends Command {

    public LitematicaCommand(IBaritone baritone) {
        super(baritone, "litematica");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        int schematic = 0;
        if (args.hasAny()) {
            args.requireMax(1);
            if (args.is(Integer.class)) {
                schematic = args.getAs(Integer.class) - 1;
            }
        }
        try {
            baritone.getBuilderProcess().buildOpenLitematic(schematic);
        } catch (IndexOutOfBoundsException e) {
            logDirect("Pleas provide a valid index.");
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Builds the loaded schematic";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Build a schematic currently open in Litematica.",
                "",
                "Usage:",
                "> litematica",
                "> litematica <#>"
        );
    }
}