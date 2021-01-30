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
import baritone.api.cache.IRememberedInventory;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandInvalidStateException;
import baritone.api.utils.BetterBlockPos;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class ChestsCommand extends Command {

    public ChestsCommand(IBaritone baritone) {
        super(baritone, "chests");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(0);
        Set<Map.Entry<BlockPos, IRememberedInventory>> entries =
                ctx.worldData().getContainerMemory().getRememberedInventories().entrySet();
        if (entries.isEmpty()) {
            throw new CommandInvalidStateException("No remembered inventories");
        }
        for (Map.Entry<BlockPos, IRememberedInventory> entry : entries) {
            // betterblockpos has censoring
            BetterBlockPos pos = new BetterBlockPos(entry.getKey());
            IRememberedInventory inv = entry.getValue();
            logDirect(pos.toString());
            for (ItemStack item : inv.getContents()) {
                ITextComponent component = item.getTextComponent();
                component.appendText(String.format(" x %d", item.getCount()));
                logDirect(component);
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Display remembered inventories";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "The chests command lists remembered inventories, I guess?",
                "",
                "Usage:",
                "> chests"
        );
    }
}
