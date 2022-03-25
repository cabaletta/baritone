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
import baritone.api.command.datatypes.BlockById;
import baritone.api.command.datatypes.ForBlockOptionalMeta;
import baritone.api.command.exception.CommandException;
import baritone.api.utils.BlockOptionalMeta;
import baritone.cache.WorldScanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Highlight blocks of a certain type
 * @author stackmagic
 * @since 2022-03-22
 */
public class HighlightCommand extends Command {

    public HighlightCommand(IBaritone baritone) {
        super(baritone, "highlight");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        List<BlockOptionalMeta> boms = new ArrayList<>();
        while (args.hasAny()) {
            boms.add(args.getDatatypeFor(ForBlockOptionalMeta.INSTANCE));
        }
        WorldScanner.INSTANCE.repack(ctx);
        logDirect(String.format("Highlighting %s", boms));
        baritone.getHighlightProcess().highlight(boms.toArray(new BlockOptionalMeta[0]));
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return args.tabCompleteDatatype(BlockById.INSTANCE);
    }

    @Override
    public String getShortDesc() {
        return "Highlight some blocks";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "The highlight command allows you to tell Baritone to search for and highlight individual blocks.",
                "They are highlighted the same way they are while the mine command is running.",
                "",
                "The specified blocks can be ores (which are commonly cached), or any other block.",
                "",
                "Usage:",
                "> highlight diamond_ore - Highlight all diamond ore it can find around you.",
                "> highlight redstone_ore lit_redstone_ore - Highlights redstone ore."
        );
    }
}
