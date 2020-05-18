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

package baritone.utils.inventory;

import java.util.List;
import java.util.Optional;

import baritone.Baritone;
import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;

/**
 * @author Matthew Carlson
 */
public class ChestHelper implements Helper {
    private final IPlayerContext ctx;

    private final Baritone baritone;

    public ChestHelper(IPlayerContext ctx, Baritone baritone) {
        this.ctx = ctx;
        this.baritone = baritone;
    }

    /**
     * @return the number trasnfer
     */
    public int transferItemsToOpenChest(ItemFilter[] itemFilter, int desiredQuantity, boolean all_at_once) {
        ClientPlayerEntity player = ctx.player();
        if (player.openContainer instanceof PlayerContainer) {
            // We opened our own inventory!
            System.out.println("We don't have a container open");
        }
        int transfer_amount = 0;
        System.out.print("Trying to transfer: ");
        for (ItemFilter item_filter : itemFilter)
            System.out.print(item_filter.toString());
        System.out.println();

        Container current_container = player.openContainer;
        int playerSlots = player.inventory.mainInventory.size();
        int numberSlots = current_container.inventorySlots.size();
        int numberChestSlots = numberSlots - playerSlots; // the number of slots that belong to the chest
        int playerSlotStartIndex = numberChestSlots; // where our player slots begin
        int emptySlots = 0;

        for (int i = 0; i < playerSlotStartIndex; i ++) {
            // Count how many empty slots are in the chest
            Slot slot = current_container.getSlot(i);
            if (slot.getStack().isEmpty()) emptySlots ++;
        }
        for (int i = playerSlotStartIndex; i < numberSlots && emptySlots > 0; i++) {
            Slot slot = current_container.getSlot(i);
            ItemStack stack = slot.getStack();
            if (itemInFilter(itemFilter, stack)) {
                // try to transfer it to the chest
                System.out.println("Attempting to stash " + stack.toString());
                int count = stack.getCount();
                if (tryQuickTransferToChest(i)) {
                    transfer_amount += count;
                    emptySlots -= 1;
                }
                else
                    System.out.println("Failed");
                if (!all_at_once)
                    return transfer_amount;
            }
        }
        return transfer_amount;
    }

    public int transferItemsToOpenChest(final List<ItemFilter> filter, int desiredQuantity, boolean all_at_once) {
        return this.transferItemsToOpenChest(filter.toArray(new ItemFilter[0]), desiredQuantity, all_at_once);
    }

    /**
     * @return true if it succeeded- the slot should be zero
     */
    public boolean tryQuickTransferToChest(int inventorySlotId) {
        // Get the actual slot in the player?
        // TODO: rework this
        Container current_container = ctx.player().openContainer;
        ItemStack stack = current_container.getSlot(inventorySlotId).getStack();
        int old_count = stack.getCount();
        // do the move
        ctx.playerController().windowClick(current_container.windowId, inventorySlotId, 0, ClickType.QUICK_MOVE,
                ctx.player());
        stack = current_container.getSlot(inventorySlotId).getStack();
        int new_count = stack.getCount();
        System.out.println("Old: " + old_count + " New:" + new_count + stack.toString());
        return old_count != new_count || stack.isEmpty();
    }

    private boolean itemInFilter(ItemFilter[] filter, Item item) {
        for (ItemFilter item_filter : filter) {
            if (item_filter.matches(item)) return true;
        }
        return false;
    }

    private boolean itemInFilter(ItemFilter[] filter, ItemStack item_stack) {
        return itemInFilter(filter, item_stack.getItem());
    }

    // Based on filter, how many items do we have in the inventory to store?
    public int numberItemsInInventory(ItemFilter[] filter) {
        NonNullList<ItemStack> inv = ctx.player().inventory.mainInventory;
        int totalCount = 0;

        // Check if we have this item
        for (ItemStack item : inv) {
            if (item == null)
                continue;
            if (itemInFilter(filter, item)) {
                totalCount += item.getCount();
            }
        }
        return totalCount;

    }

    // Based on filter, what is the size of the largest stack?
    public int getSizeOfLargestStackOfItemsInInventory(ItemFilter[] filter) {
        NonNullList<ItemStack> inv = ctx.player().inventory.mainInventory;
        int largestStack = 0;
        for (ItemFilter item_filter : filter) {
            for (ItemStack item : inv) {
                if (item == null)
                    continue;
                if (item_filter.matches(item.getItem())) {
                    largestStack = (item.getCount() > largestStack) ? item.getCount() : largestStack;
                }
            }
        }
        return largestStack;
    }

    /**
     * Used to open a chest or placed shulker box
     * @return true when we finished, false when we still aren't done
     */
    public boolean rightClickOpenChest(BlockPos pos) {
        Optional<Rotation> reachable = RotationUtils.reachable(ctx.player(), pos,
                ctx.playerController().getBlockReachDistance());
        if (reachable.isPresent()) {
            baritone.getLookBehavior().updateTarget(reachable.get(), true);
            if (pos.equals(ctx.getSelectedBlock().orElse(null))) {
                baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true); // TODO find some way to
                                                                                                // right click even if
                                                                                                // we're in an ESC menu
                if (!(ctx.player().openContainer instanceof PlayerContainer)) { // check to make sure we didn't open the
                                                                                // player container
                    baritone.getInputOverrideHandler().clearAllKeys();
                    return true;
                }
            }
            return false; // trying to right click, will do it next tick or so
        }
        System.out.println("Arrived but failed to right click open");
        return true;
    }

}