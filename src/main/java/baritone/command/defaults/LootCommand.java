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
import baritone.api.command.exception.CommandException;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;


public class LootCommand extends Command {
    protected LootCommand(IBaritone baritone) {
        super(baritone, "loot", "steal");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        int amount;
        if (args.hasExactly(0)) {
            amount = -1;
        } else {
            amount = args.get().getAs(int.class);
        }
        List<Item> items = new ArrayList<>();
        if (args.hasAny()) {
            do {
                items.add(Item.getByNameOrId(args.getString()));
            } while (args.hasAny());
        } else {
            items = null;
        }
        baritone.getLootProcess().loot(amount, items);
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        return args.tabCompleteDatatype(BlockById.INSTANCE);
    }

    @Override
    public String getShortDesc() {
        return "Loot containers";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "The loot command allows you to tell Baritone to search for and loot nearby containers.",
                "",
                "If no item(s) are specified, Baritone steals all items from every container",
                "",
                "You can change the list of containers that Baritone loots in the lootContainers setting.",
                "",
                "Usage:",
                "> loot - Loot all containers it can find until stopped.",
                "> loot -1 - Same as loot without arguments",
                "> loot <amount> - Loot amount of containers that are closest.",
                "> loot <amount> <item> [item] [item]... - Loot specific items from the 25 closest containers."
        );
    }
}
