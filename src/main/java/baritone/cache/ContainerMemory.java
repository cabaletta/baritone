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

package baritone.cache;

import baritone.api.cache.IContainerMemory;
import baritone.api.cache.IRememberedInventory;
import baritone.api.utils.IPlayerContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.nio.file.Path;
import java.util.*;

public class ContainerMemory implements IContainerMemory {
    public ContainerMemory(Path saveTo) {
        // eventually
    }

    /**
     * The current remembered inventories
     */
    private final Map<BlockPos, RememberedInventory> inventories = new HashMap<>();

    public synchronized void setup(BlockPos pos, int windowId, int slotCount) {
        RememberedInventory inventory = inventories.computeIfAbsent(pos, x -> new RememberedInventory());
        inventory.windowId = windowId;
        inventory.size = slotCount;
    }

    public synchronized Optional<RememberedInventory> getInventoryFromWindow(int windowId) {
        return inventories.values().stream().filter(i -> i.windowId == windowId).findFirst();
    }

    @Override
    public final synchronized RememberedInventory getInventoryByPos(BlockPos pos) {
        return inventories.get(pos);
    }

    @Override
    public final synchronized Map<BlockPos, IRememberedInventory> getRememberedInventories() {
        // make a copy since this map is modified from the packet thread
        return new HashMap<>(inventories);
    }

    /**
     * An inventory that we are aware of.
     * <p>
     * Associated with a {@link BlockPos} in {@link WorldDataContainer#rememberedInventories}.
     */
    public static class RememberedInventory implements IRememberedInventory {

        /**
         * The list of items in the inventory
         */
        private final List<ItemStack> items;

        /**
         * The last known window ID of the inventory
         */
        private int windowId;

        /**
         * The size of the inventory
         */
        private int size;

        private RememberedInventory() {
            this.items = new ArrayList<>();
        }

        @Override
        public final List<ItemStack> getContents() {
            return Collections.unmodifiableList(this.items);
        }

        @Override
        public final int getSize() {
            return this.size;
        }

        public int getWindowId() {
            return this.windowId;
        }

        public void updateFromOpenWindow(IPlayerContext ctx) {
            items.clear();
            items.addAll(ctx.player().openContainer.getInventory().subList(0, size));
            System.out.println("Saved " + items);
        }
    }
}
