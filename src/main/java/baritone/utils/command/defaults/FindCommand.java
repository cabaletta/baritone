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
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.command.Command;
import baritone.api.utils.command.datatypes.BlockById;
import baritone.api.utils.command.exception.CommandException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import net.minecraft.block.Block;
import net.minecraft.util.registry.IRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class FindCommand extends Command {

    public FindCommand(IBaritone baritone) {
        super(baritone, "find");
    }

    @Override
    protected void executed(String label, ArgConsumer args) throws CommandException {
        List<Block> toFind = new ArrayList<>();
        while (args.hasAny()) {
            toFind.add(args.getDatatypeFor(BlockById.INSTANCE));
        }
        BetterBlockPos origin = ctx.playerFeet();
        toFind.stream()
                .flatMap(block ->
                        ctx.worldData().getCachedWorld().getLocationsOf(
                                IRegistry.BLOCK.getKey(block).getPath(),
                                Integer.MAX_VALUE,
                                origin.x,
                                origin.y,
                                4
                        ).stream()
                )
                .map(BetterBlockPos::new)
                .map(BetterBlockPos::toString)
                .forEach(this::logDirect);
    }

    @Override
    protected Stream<String> tabCompleted(String label, ArgConsumer args) {
        return args.tabCompleteDatatype(BlockById.INSTANCE);
    }

    @Override
    public String getShortDesc() {
        return "Find positions of a certain block";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "",
                "",
                "Usage:",
                "> "
        );
    }
}
