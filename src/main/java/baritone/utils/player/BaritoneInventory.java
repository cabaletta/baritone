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

package baritone.utils.player;

import baritone.api.utils.IBaritoneInventory;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.InventorySlot;
import baritone.api.utils.Pair;
import net.minecraft.item.ItemStack;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Brady
 * @since 7/4/2023
 */
public final class BaritoneInventory implements IBaritoneInventory {

    private final IPlayerContext ctx;

    public BaritoneInventory(IPlayerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Stream<Pair<InventorySlot, ItemStack>> allSlots() {
        return Stream.concat(this.hotbarSlots(), Stream.concat(Stream.of(this.offhand()), this.inventorySlots()));
    }

    @Override
    public Stream<Pair<InventorySlot, ItemStack>> hotbarSlots() {
        return IntStream.range(0, 9).mapToObj(InventorySlot::inventory).map(this::itemSlotPairAt);
    }

    @Override
    public Stream<Pair<InventorySlot, ItemStack>> inventorySlots() {
        return IntStream.range(9, 36).mapToObj(InventorySlot::inventory).map(this::itemSlotPairAt);
    }

    @Override
    public Pair<InventorySlot, ItemStack> offhand() {
        return new Pair<>(InventorySlot.offhand(), ctx.player().inventory.offHandInventory.get(0));
    }

    @Override
    public Stream<Pair<InventorySlot, ItemStack>> armorSlots() {
        return IntStream.range(0, 4).mapToObj(InventorySlot::armor).map(this::itemSlotPairAt);
    }

    @Override
    public ItemStack itemAt(InventorySlot slot) {
        return ctx.player().inventoryContainer.getSlot(slot.getSlotId()).getStack();
    }

    private Pair<InventorySlot, ItemStack> itemSlotPairAt(InventorySlot slot) {
        return new Pair<>(slot, this.itemAt(slot));
    }
}
