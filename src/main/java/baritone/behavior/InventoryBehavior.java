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

package baritone.behavior;

import baritone.Baritone;
import baritone.api.event.events.TickEvent;
import baritone.api.utils.Helper;
import baritone.utils.InventorySlot;
import baritone.utils.ToolSet;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.*;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.OptionalInt;
import java.util.Random;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

public final class InventoryBehavior extends Behavior implements Helper {

    private int ticksSinceLastInventoryMove;
    private int[] lastTickRequestedMove; // not everything asks every tick, so remember the request while coming to a halt

    public InventoryBehavior(Baritone baritone) {
        super(baritone);
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT) {
            return;
        }
        ticksSinceLastInventoryMove++;

        // TODO: Move these checks into "requestSwapWithHotBar" or whatever supersedes it
        if (!this.canAccessInventory()) {
            return;
        }
        if (ctx.player().openContainer != ctx.player().inventoryContainer) {
            // we have a crafting table or a chest or something open
            return;
        }

        if (Baritone.settings().allowHotbarManagement.value && baritone.getPathingBehavior().isPathing()) {
            // TODO: Some way of indicating which slots are currently reserved by this setting
            if (firstValidThrowaway() >= 9) { // aka there are none on the hotbar, but there are some in main inventory
                requestSwapWithHotBar(firstValidThrowaway(), 8);
            }
            int pick = bestToolAgainst(Blocks.STONE, ItemPickaxe.class);
            if (pick >= 9) {
                requestSwapWithHotBar(pick, 0);
            }
        }

        if (lastTickRequestedMove != null) {
            logDebug("Remembering to move " + lastTickRequestedMove[0] + " " + lastTickRequestedMove[1] + " from a previous tick");
            requestSwapWithHotBar(lastTickRequestedMove[0], lastTickRequestedMove[1]);
        }
    }

    public boolean attemptToPutOnHotbar(int inMainInvy, IntPredicate disallowedHotbar) {
        OptionalInt destination = getTempHotbarSlot(disallowedHotbar);
        if (destination.isPresent()) {
            if (!requestSwapWithHotBar(inMainInvy, destination.getAsInt())) {
                return false;
            }
        }
        return true;
    }

    public OptionalInt getTempHotbarSlot(IntPredicate disallowedHotbar) {
        // we're using 0 and 8 for pickaxe and throwaway
        ArrayList<Integer> candidates = new ArrayList<>();
        for (int i = 1; i < 8; i++) {
            if (ctx.player().inventory.mainInventory.get(i).isEmpty() && !disallowedHotbar.test(i)) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) {
            for (int i = 1; i < 8; i++) {
                if (!disallowedHotbar.test(i)) {
                    candidates.add(i);
                }
            }
        }
        if (candidates.isEmpty()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(candidates.get(new Random().nextInt(candidates.size())));
    }

    private boolean requestSwapWithHotBar(int inInventory, int inHotbar) {
        lastTickRequestedMove = new int[]{inInventory, inHotbar};
        if (ticksSinceLastInventoryMove < Baritone.settings().ticksBetweenInventoryMoves.value) {
            logDebug("Inventory move requested but delaying " + ticksSinceLastInventoryMove + " " + Baritone.settings().ticksBetweenInventoryMoves.value);
            return false;
        }
        if (Baritone.settings().inventoryMoveOnlyIfStationary.value && !baritone.getInventoryPauserProcess().stationaryForInventoryMove()) {
            logDebug("Inventory move requested but delaying until stationary");
            return false;
        }
        ctx.playerController().windowClick(ctx.player().inventoryContainer.windowId, inInventory < 9 ? inInventory + 36 : inInventory, inHotbar, ClickType.SWAP, ctx.player());
        ticksSinceLastInventoryMove = 0;
        lastTickRequestedMove = null;
        return true;
    }

    private int firstValidThrowaway() { // TODO offhand idk
        NonNullList<ItemStack> invy = ctx.player().inventory.mainInventory;
        for (int i = 0; i < invy.size(); i++) {
            if (this.isThrowawayItem(invy.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private int bestToolAgainst(Block against, Class<? extends ItemTool> cla$$) {
        NonNullList<ItemStack> invy = ctx.player().inventory.mainInventory;
        int bestInd = -1;
        double bestSpeed = -1;
        for (int i = 0; i < invy.size(); i++) {
            ItemStack stack = invy.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (Baritone.settings().itemSaver.value && (stack.getItemDamage() + Baritone.settings().itemSaverThreshold.value) >= stack.getMaxDamage() && stack.getMaxDamage() > 1) {
                continue;
            }
            if (cla$$.isInstance(stack.getItem())) {
                double speed = ToolSet.calculateSpeedVsBlock(stack, against.getDefaultState()); // takes into account enchants
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestInd = i;
                }
            }
        }
        return bestInd;
    }

    public boolean hasGenericThrowaway() {
        return this.canSelectItem(this::isThrowawayItem);
    }

    public boolean selectThrowawayForLocation(boolean select, int x, int y, int z) {
        final Predicate<Predicate<? super ItemStack>> op = select ? this::trySelectItem : this::canSelectItem;

        final IBlockState maybe = baritone.getBuilderProcess().placeAt(x, y, z, baritone.bsi.get0(x, y, z));
        if (maybe != null) {
            return op.test(stack -> {
                if (!(stack.getItem() instanceof ItemBlock)) {
                    return false;
                }
                Block block = ((ItemBlock) stack.getItem()).getBlock();
                return maybe.equals(block.getStateForPlacement(
                        ctx.world(),
                        ctx.playerFeet(),
                        EnumFacing.UP,
                        0.5f, 1.0f, 0.5f,
                        stack.getItem().getMetadata(stack.getMetadata()),
                        ctx.player()
                ));
            }) || op.test(stack -> {
                // Since a stack didn't match the desired block state, accept a match of just the block
                return stack.getItem() instanceof ItemBlock
                        && ((ItemBlock) stack.getItem()).getBlock().equals(maybe.getBlock());
            });
        }
        return op.test(this::isThrowawayItem);
    }

    public boolean canSelectItem(Predicate<? super ItemStack> desired) {
        return this.resolveSelectionStrategy(desired) != null;
    }

    public boolean trySelectItem(Predicate<? super ItemStack> desired) {
        final SelectionStrategy strategy = this.resolveSelectionStrategy(desired);
        if (strategy != null) {
            strategy.run();
            // TODO: Consider cases where returning the SelectionType is needed/useful to the caller
            return true;
        }
        return false;
    }

    public SelectionStrategy resolveSelectionStrategy(Predicate<? super ItemStack> desired) {
        final InventorySlot slot = this.findSlotMatching(desired);
        if (slot != null) {
            switch (slot.getType()) {
                case HOTBAR:
                    return SelectionStrategy.of(SelectionType.IMMEDIATE, () ->
                            ctx.player().inventory.currentItem = slot.getInventoryIndex());
                case OFFHAND:
                    // main hand takes precedence over off hand
                    // that means that if we have block A selected in main hand and block B in off hand, right clicking places block B
                    // we've already checked above ^ and the main hand can't possible have an acceptablethrowawayitem
                    // so we need to select in the main hand something that doesn't right click
                    // so not a shovel, not a hoe, not a block, etc
//                    for (int i = 0; i < 9; i++) {
//                        ItemStack item = ctx.player().inventory.mainInventory.get(i);
//                        if (item.isEmpty() || item.getItem() instanceof ItemPickaxe) {
//                            ctx.player().inventory.currentItem = i;
//                            break;
//                        }
//                    }
                    // TODO: This method of acquiring an offhand item is temporary and just guarantees that there won't
                    //       be any item usage issues. It's probably worth adding a parameter to this method so that the
                    //       caller can indicate what the purpose of the item is. for example, attacks are always done
                    //       using the main hand.
                    // If there is an empty slot on the hotbar, switch to that slot and swap the offhand into it
                    final InventorySlot empty = this.findSlotMatching(ItemStack::isEmpty);
                    if (empty != null && empty.getType() == InventorySlot.Type.HOTBAR) {
                        return SelectionStrategy.of(SelectionType.ENQUEUED, () -> {
                            ctx.player().inventory.currentItem = empty.getInventoryIndex();
                            ctx.playerController().syncHeldItem();
                            ctx.player().connection.sendPacket(new CPacketPlayerDigging(
                                    CPacketPlayerDigging.Action.SWAP_HELD_ITEMS,
                                    BlockPos.ORIGIN,
                                    EnumFacing.DOWN
                            ));
                        });
                    }
                    break;
                case INVENTORY:
                    if (this.canAccessInventory()) {
                        return SelectionStrategy.of(SelectionType.ENQUEUED, () -> {
                            // TODO: Determine if hotbar swap can be immediate, and return type accordingly
                            requestSwapWithHotBar(slot.getInventoryIndex(), 7);
                            ctx.player().inventory.currentItem = 7;
                        });
                    }
                    break;
            }
        }
        return null;
    }

    /**
     * Returns an {@link InventorySlot} that contains a stack matching the given predicate. The priority of the
     * returned slot is the hotbar, offhand, and finally the main inventory, if {@link #canAccessInventory()} is
     * {@code true}. Additionally, for the hotbar and main inventory, slots with a lower index will be returned.
     *
     * @param desired The predicate to match
     * @return A matching slot, or {@code null} if none.
     */
    public InventorySlot findSlotMatching(final Predicate<? super ItemStack> desired) {
        final EntityPlayerSP p = ctx.player();
        final NonNullList<ItemStack> inv = p.inventory.mainInventory;

        for (int i = 0; i < 9; i++) {
            ItemStack item = inv.get(i);
            if (desired.test(item)) {
                return InventorySlot.hotbar(i);
            }
        }

        if (desired.test(p.inventory.offHandInventory.get(0))) {
            return InventorySlot.offhand();
        }

        if (this.canAccessInventory()) {
            for (int i = 9; i < 36; i++) {
                if (desired.test(inv.get(i))) {
                    return InventorySlot.inventory(i);
                }
            }
        }
        return null;
    }

    public boolean canAccessInventory() {
        return Baritone.settings().allowInventory.value;
    }

    public boolean isThrowawayItem(ItemStack stack) {
        return this.isThrowawayItem(stack.getItem());
    }

    public boolean isThrowawayItem(Item item) {
        return Baritone.settings().acceptableThrowawayItems.value.contains(item);
    }

    public interface SelectionStrategy extends Runnable {

        @Override
        void run();

        SelectionType getType();

        static SelectionStrategy of(final SelectionType type, final Runnable runnable) {
            return new SelectionStrategy() {
                @Override
                public void run() {
                    runnable.run();
                }

                @Override
                public SelectionType getType() {
                    return type;
                }
            };
        }
    }

    public enum SelectionType {
        IMMEDIATE, ENQUEUED
    }
}
