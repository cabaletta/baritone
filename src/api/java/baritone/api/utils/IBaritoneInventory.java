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

package baritone.api.utils;

import net.minecraft.item.ItemStack;

import java.util.stream.Stream;

/**
 * @author Brady
 */
public interface IBaritoneInventory {

    /**
     * Returns a stream containing all the player's regular inventory slots and items. The elements of the stream are in
     * the order of hotbar, offhand, then main inventory, for a total of 37 slots. This explicitly does not contain the
     * armor slots or crafting grid, which may otherwise be accessed with {@link #armorSlots()} and/or {@link #itemAt}.
     *
     * @return All the player's inventory slots and items
     */
    Stream<Pair<InventorySlot, ItemStack>> allSlots();

    Stream<Pair<InventorySlot, ItemStack>> hotbarSlots();

    Stream<Pair<InventorySlot, ItemStack>> inventorySlots();

    Pair<InventorySlot, ItemStack> offhand();

    Stream<Pair<InventorySlot, ItemStack>> armorSlots();

    ItemStack itemAt(InventorySlot slot);
}
