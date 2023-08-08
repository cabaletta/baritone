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

package baritone.api.process;

import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;

import java.util.List;

/**
 * Allows you to craft items.
 */
public interface ICraftingProcess extends IBaritoneProcess {

    /**
     * Executes the crafting of the requested recipes such that we obtain the requested amount of output.
     * It is possible to mix recipes with different out puts, but it may lead to strange results.
     * @param recipes List of recipes that should be used.
     * @param amount how many result items are wanted.
     */
    void craft(List<IRecipe> recipes, int amount);

    /**
     * @param item that should be crafted.
     * @param allCraftingRecipes if all crafting recipes should be returned or only the one that can be crafted
     * @return List of recipes that result in the provided Item.
     */
    List<IRecipe> getCraftingRecipes(Item item, boolean allCraftingRecipes);

    /**
     * @param recipes that should be crafted.
     * @param amount how much output is wanted.
     * @return Can we craft this the requested amount of output from the provided recipes?
     */
    boolean canCraft(List<IRecipe> recipes, int amount);

    /**
     * @param recipe the recipe we want to craft.
     * @return Do we need a crafting table or can we craft it in our inventory?
     */
    boolean canCraftInInventory(IRecipe recipe);
}
