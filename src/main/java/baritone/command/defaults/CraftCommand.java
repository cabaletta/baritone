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
import net.minecraft.item.Item;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class CraftCommand extends Command {

    protected CraftCommand(IBaritone baritone) {
        super(baritone, "craft");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        //todo works for simple items like "stick" but items like "granite" aren't parsed because they are stone:1
        Item item = Item.getByNameOrId(args.getString());

        int amount = args.hasAny() ? args.getAs(Integer.class) : 1;

        if (item == null) {
            logDirect("invalid Item");
        } else if (!baritone.getCraftingProcess().hasCraftingRecipe(item)) {
            logDirect("no crafting recipe for "+item.getTranslationKey()+" found.");
            /*todo missing feature check if we can craft before we walk to a crafting table
        } else if (BaritoneAPI.getSettings().allowAutoCraft.value && !baritone.getCraftingProcess().canCraft(item, amount)) {
            logDirect("trying to craft "+ item.getTranslationKey());
            logDirect("not enough resources in inventory");
            /**/
        } else {
            baritone.getCraftingProcess().craft(item, amount);
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        //todo add autocomplete for items
        return null;
    }
  
    @Override
    public String getShortDesc() {
        return "Craft a item.";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Go to a crafting table and craft a item.",
                "",
                "Usage:",
                "> craft <item> <amount> - Go to a crafting table, and craft a item."
        );
    }
}