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
import baritone.api.command.datatypes.ItemById;
import baritone.api.command.exception.CommandException;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class CraftCommand extends Command {

    protected CraftCommand(IBaritone baritone) {
        super(baritone, "craft");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        int amount = args.getAsOrDefault(Integer.class, 1);
        Item item = args.getDatatypeForOrNull(ItemById.INSTANCE);
        List<IRecipe> recipes;

        if (item != null) {
            recipes = baritone.getCraftingProcess().getCraftingRecipes(item, false);
        } else {
            String itemName = args.rawRest();
            recipes = new ArrayList<>();
            for (IRecipe recipe : CraftingManager.REGISTRY) {
                if (recipe.getRecipeOutput().getDisplayName().equalsIgnoreCase(itemName)) {
                    recipes.add(recipe);
                }
            }
        }

        if (recipes.isEmpty()) {
            logDirect("no crafting recipe found.");
        } else if (!baritone.getCraftingProcess().canCraft(recipes, amount)) {
            logDirect("Insufficient Resources");
        } else {
            baritone.getCraftingProcess().craft(recipes, amount);
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        while (args.has(2)) {
            if (args.peekDatatypeOrNull(ItemById.INSTANCE) == null) {
                return Stream.empty();
            }
            args.get();
        }
        return args.tabCompleteDatatype(ItemById.INSTANCE);
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
                "> craft [quantity] <item>  - Go to a crafting table, and craft a item.",
                "Examples:",
                "> craft 17 planks -> will craft 20 planks out of any logs you have.",
                "> craft oak wood planks -> will craft 4 oak wood planks."
        );
    }
}