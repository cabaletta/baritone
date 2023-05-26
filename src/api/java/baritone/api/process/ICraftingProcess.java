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
 * but it rescans the world every once in a while so it doesn't get fooled by its cache
 */
public interface ICraftingProcess extends IBaritoneProcess {

    /**
     * @param item that should be crafted
     * @return Can the item be crafted in a crafting table?
     */
    boolean hasCraftingRecipe(Item item);

    /**
     * @param item that should be crafted
     * @return List of all recipies that result in the provided Item.
     */
    List<IRecipe> getCraftingRecipes(Item item);

    /**
     * Checks if the requested item can be crafted the requested amount of times.
     * @param item that should be crafted
     * @param amount how much of that item is wanted.
     * @return
     */
    boolean canCraft(Item item, int amount);

    /**
     * Executes the crafting of the requested item the requested amount of times.
     * @param item that should be crafted
     * @param amount how much of that item is wanted.
     */
    void craft(Item item, int amount);
}
