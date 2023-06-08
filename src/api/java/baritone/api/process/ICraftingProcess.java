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
     * Executes the crafting of the requested item such that we obtain the requested amount.
     * @param item that should be crafted
     * @param amount how much of that item is wanted.
     */
    void craftItem(Item item, int amount);

    /**
     * Executes the crafting of the requested recipe such that we obtain the requested amount of output.
     * @param recipe recipe that should be used.
     * @param amount how many result items are wanted.
     */
    void craftRecipe(IRecipe recipe, int amount);

    /**
     * @param item that should be crafted.
     * @return Can the item be crafted in a crafting table?
     */
    boolean hasCraftingRecipe(Item item);

    /**
     * @param item that should be crafted.
     * @return List of all recipes that result in the provided Item.
     */
    List<IRecipe> getCraftingRecipes(Item item);

    /**
     * @param item that should be crafted.
     * @param amount how much of this item should be crafted.
     * @return Can we craft this item the requested amount of times?
     */
    boolean canCraft(Item item, int amount);

    /**
     * @param recipe that should be crafted.
     * @param amount how much output is wanted.
     * @return Can we craft this recipe to get the requested amount of output?
     */
    boolean canCraft(IRecipe recipe, int amount);

    /**
     * @param recipe the recipe we want to craft.
     * @return Do we need a crafting table or can we craft it in our inventory?
     */
    boolean canCraftInInventory(IRecipe recipe);
}
