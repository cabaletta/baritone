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

import java.util.ArrayList;
import java.util.List;

import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerContext;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

/**
 * @author Matthew Carlson
 */
public class InventoryHelper implements Helper {
    private final IPlayerContext ctx;

    public InventoryHelper(final IPlayerContext ctx) {
        this.ctx = ctx;
    }

    public boolean isInventoryFull() {
        return ctx.player().inventory.getFirstEmptyStack() == -1;
    }

    public void swapWithHotBar(final int inInventory, final int inHotbar) {
        ctx.playerController().windowClick(ctx.player().container.windowId,
                inInventory < 9 ? inInventory + 36 : inInventory, inHotbar, ClickType.SWAP, ctx.player());
    }

    // Shamelessly stolen from PlayerInventory.class vanilla minecraft
    public boolean canMergeStacks(final ItemStack dest, final ItemStack src) {
        return !dest.isEmpty() && this.stackEqualExact(dest, src) && dest.isStackable()
                && dest.getCount() < dest.getMaxStackSize()
                && dest.getCount() < ctx.player().inventory.getInventoryStackLimit();
    }

    /**
     * Checks if we can merge them into each other and it will matter
     */
    public boolean canMergeStacksCompletely(final ItemStack stack1, final ItemStack stack2) {
        if (!canMergeStacks(stack1, stack2))
            return false;
        return (stack1.getCount() + stack2.getCount() < stack1.getMaxStackSize());
    }

    /**
     * Checks item, NBT, and meta if the item is not damageable
     */
    public boolean stackEqualExact(final ItemStack stack1, final ItemStack stack2) {
        return stack1.getItem() == stack2.getItem() && ItemStack.areItemStackTagsEqual(stack1, stack2);
    }

    // Based on filter, how many items do we have in the inventory to store?
    public int numberItemsInInventory(final List<ItemFilter> filter) {
        return this.numberItemsInInventory(filter.toArray(new ItemFilter[0]));
    }

    public int numberItemsInInventory(final ItemFilter[] filter) {
        final NonNullList<ItemStack> inv = ctx.player().inventory.mainInventory;
        int totalCount = 0;
        for (final ItemFilter item_filter : filter) {
            // Check if we have this item
            for (final ItemStack item : inv) {
                if (item == null)
                    continue;
                if (item_filter.matches(item.getItem())) {
                    totalCount += item.getCount();
                }
            }
        }
        return totalCount;
    }

    // Based on filter, what is the size of the largest stack?
    public int getSizeOfLargestStackOfItemsInInventory(final List<ItemFilter> filter) {
        return this.getSizeOfLargestStackOfItemsInInventory(filter.toArray(new ItemFilter[0]));
    }

    public int getSizeOfLargestStackOfItemsInInventory(final ItemFilter[] filter) {
        final NonNullList<ItemStack> inv = ctx.player().inventory.mainInventory;
        int largestStack = 0;
        for (final ItemFilter item_filter : filter) {
            for (final ItemStack item : inv) {
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
     * @return -1 if no stacks were found TODO: should this return optional? That seems to be a common pattern here
     */
    public int getSizeOfSmallestStackOfItemsInInventory(final List<ItemFilter> filter) {
        return this.getSizeOfSmallestStackOfItemsInInventory(filter.toArray(new ItemFilter[0]));
    }

    public int getSizeOfSmallestStackOfItemsInInventory(final ItemFilter[] filter) {
        final NonNullList<ItemStack> inv = ctx.player().inventory.mainInventory;
        int largestStack = -1;
        for (final ItemFilter item_filter : filter) {
            for (final ItemStack item : inv) {
                if (item == null)
                    continue;
                if (item.getItem().equals(item_filter.i)) {
                    largestStack = (item.getCount() > largestStack || largestStack == -1) ? item.getCount()
                            : largestStack;
                }
            }
        }
        return largestStack;
    }

    public boolean attemptToCondense() {
        // Don't mess with items on the hotbar
        final int endIndex = ctx.player().inventory.mainInventory.size() - 1;

        if (ctx.player().openContainer != ctx.player().container)
            return false; // if we have something open
        for (int condenseIndex = endIndex; condenseIndex > 9; condenseIndex--) {
            final int srcInInventory = condenseIndex < 9 ? condenseIndex + 36 : condenseIndex;
            int destIndex = -1;
            final ItemStack srcStack = ctx.player().inventory.mainInventory.get(condenseIndex);
            ItemStack destStack = null;
            boolean full_merge = false;
            // TODO figure out where to place it,
            for (int i = 9; i < condenseIndex && destIndex == -1; i++) {
                destStack = ctx.player().inventory.mainInventory.get(i);
                if (this.canMergeStacks(destStack, srcStack)) {
                    destIndex = i;
                    // check if we can merge the stacks completely
                    full_merge = this.canMergeStacksCompletely(srcStack, destStack);
                }

            }
            if (destIndex != -1) {
                System.out.println("Attempting to condense: " + srcStack + "@" + condenseIndex + " into " + destStack
                        + "@" + destIndex);
                final int dstInInventory = destIndex < 9 ? destIndex + 36 : destIndex;
                ctx.playerController().windowClick(ctx.player().container.windowId, srcInInventory, 0, ClickType.PICKUP,
                        ctx.player());
                ctx.playerController().windowClick(ctx.player().container.windowId, dstInInventory, 0, ClickType.PICKUP,
                        ctx.player());
                // if we are unable to place all the blocks, place them back where they started
                if (!full_merge)
                    ctx.playerController().windowClick(ctx.player().container.windowId, srcInInventory, 0,
                            ClickType.PICKUP, ctx.player());
                return true;
            }
        }
        return false;
    }

}