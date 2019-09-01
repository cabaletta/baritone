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
import baritone.api.cache.IRememberedInventory;
import baritone.api.utils.command.Command;
import baritone.api.utils.command.exception.CommandInvalidStateException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class ChestsCommand extends Command {
    public ChestsCommand() {
        super("chests", "Display remembered inventories");
    }

    @Override
    protected void executed(String label, ArgConsumer args, Settings settings) {
        args.requireMax(0);
        Set<Map.Entry<BlockPos, IRememberedInventory>> entries =
            ctx.worldData().getContainerMemory().getRememberedInventories().entrySet();

        if (entries.isEmpty()) {
            throw new CommandInvalidStateException("No remembered inventories");
        }

        for (Map.Entry<BlockPos, IRememberedInventory> entry : entries) {
            BlockPos pos = entry.getKey();
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
    protected Stream<String> tabCompleted(String label, ArgConsumer args, Settings settings) {
        return Stream.empty();
    }

    @Override
    public List<String> getLongDesc() {
        return asList(
            "The chests command lists remembered inventories, I guess?",
            "",
            "Usage:",
            "> chests"
        );
    }
}