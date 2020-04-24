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

package baritone.process;

import baritone.Baritone;
import baritone.api.pathing.goals.*;
import baritone.api.process.IInventoryStoreProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.*;
import baritone.api.utils.input.Input;
import baritone.cache.CachedChunk;
import baritone.cache.WorldScanner;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.MovementHelper;
import baritone.utils.BaritoneProcessHelper;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.stream.Collectors;

import static baritone.api.pathing.movement.ActionCosts.COST_INF;

// Split this into a store and a obtain process?
// How do you prevent them from competing?

/**
 * Stores blocks of a certain type if your inventory becomes full It will
 * attempt to store excess throwaway blocks, as well as extra storage blocks If
 * it cannot use shulker chests and it cannot use chests, it will just throw
 * away the acceptableThrowawayItems but will try to keep at least one stack
 * 
 * @author matthewfcarlson
 */
public final class InventoryStoreProcess extends BaritoneProcessHelper implements IInventoryStoreProcess {

    private BlockOptionalMetaLookup filter; // TODO make this an item filter, not a block filter
    final int DELAY_TICKS = 50; // how many ticks we wait after the inventory is full before we start doing
                                // something
    private int desiredQuantity = -1; // -1 means no desire, 0 means store as much as you can, >0 means store up to
                                      // that much
    private int tickCount = 0;

    private enum StoreState {
        IDLE, FULL, // We are full, after DELAY_TICKS, go to STORING
        STORING, CHECK_FOR_SHULKER, FIND_CHEST, GO_TO_CHEST, AT_CHEST, DISCARDING, DONE
    }

    private StoreState state = StoreState.IDLE;

    public InventoryStoreProcess(Baritone baritone) {
        super(baritone);
    }

    private boolean isInventoryFull() {
        return ctx.player().inventory.getFirstEmptyStack() == -1;
    }

    // Checks whether we have any shulkers in our inventory
    private boolean isShulkerInInventory() {
        return false;
    }

    @Override
    public boolean isActive() {
        if (ctx.player() == null || ctx.world() == null) {
            return false;
        }
        if (!Baritone.settings().storeExcessInventory.value) {
            return false;
        }

        logDebug("InventoryStoreProcess: " + this.state + " " + this.tickCount);

        // check if our inventory is full
        if (!Baritone.settings().allowInventory.value) {
            logDirect("storeExcessInventory cannot be used with allowInventory false");
            Baritone.settings().storeExcessInventory.value = false;
            return false;
        }

        // Wait until our tick count goes above delay ticks if we're done, then go back
        // to idle
        if (this.state == StoreState.DONE) {
            this.tickCount += 1;
            if (this.tickCount >= DELAY_TICKS) {
                this.state = StoreState.IDLE;
                this.tickCount = 0;
            }
            return false;
        }
        // if we're not idle, we are active
        if (this.state != StoreState.IDLE && this.state != StoreState.FULL)
            return true;
        // If the user is mucking around with their inventory, a chest, or a crafting
        // table, don't increment the ticker
        // Note: this doesn't apply when we're active
        if (ctx.player().openContainer != ctx.player().container) {
            logDebug("We have something open " + ctx.player().openContainer.toString());
            this.tickCount = 0; // TODO: move this to seperate method, resetTicker()?
            return false;
        }
        // Add to our tick count
        if (this.tickCount < DELAY_TICKS) {
            this.tickCount += 1;
            return false;
        }

        // Reset out tick count
        this.tickCount = 0;
        // If we're not full, we're going back to idle
        if (!this.isInventoryFull()) {
            this.state = StoreState.IDLE;
            return false;
        }
        // If we're in the full state, it's time to start storing
        if (this.state == StoreState.FULL) {
            this.desiredQuantity = 10;
            this.state = StoreState.STORING; // set our state to STORING
            return true;
        } else {
            this.state = StoreState.FULL;
            return false;
        }

        // If we don't have any empty slots left
        /*
         * This code figures out what we can throw away this.desiredQuantity = 0;
         * List<Item> throwAwayItems =
         * Baritone.settings().acceptableThrowawayItems.value; List<Item> wantedItems =
         * Baritone.settings().itemsToStore.value; // set the filter up so that it will
         * look for items in the inventory Set<Block> filteredItems = new HashSet<>();
         * for (Item item : throwAwayItems) {
         * filteredItems.add(Block.getBlockFromItem(item)); } for (Item item :
         * wantedItems) { filteredItems.add(Block.getBlockFromItem(item)); }
         * 
         * this.filter = new BlockOptionalMetaLookup(new ArrayList<>(filteredItems));
         * int storableCount = numberThingsAvailableToStore(); if (storableCount > 0){
         * // We need to figure out logDirect("storeExcessInventory- " + storableCount +
         * " items"); this.desiredQuantity = storableCount; this.store =
         * StoreState.SS_CHECK_FOR_SHULKER; return true; } }
         */
    }

    // Based on filter, how many items do we have in the inventory to store?
    private int numberThingsAvailableToStore() {
        NonNullList<ItemStack> inv = ctx.player().inventory.mainInventory;
        int totalCount = 0;
        for (BlockOptionalMeta block : this.filter.blocks()) {
            // Check if we have this item
            for (ItemStack item : inv) {
                if (block.matches(item)) {
                    totalCount += item.getCount();
                }
            }
        }
        return totalCount;
    }

    @Override
    public double priority() {
        return 2; // sort of arbitrary but it seems like a good amount?
    }

    @Override
    public void onLostControl() {
        this.state = StoreState.IDLE; // we're gonna drop back to idle
        this.tickCount = 0; // make sure to reset our counter
    }

    @Override
    public String displayName0() {
        // TODO figure out if we're storing or obtaining
        return "Inventory Store" + filter.toString();
    }

    /**
     * TODO: move this to obtain Scans for and returns the block pos off the scanned
     * items
     */
    public List<BlockPos> droppedItemsScan() {
        List<BlockPos> ret = new ArrayList<>();
        for (Entity entity : ((ClientWorld) ctx.world()).getAllEntities()) {
            if (entity instanceof ItemEntity) {
                ItemEntity ei = (ItemEntity) entity;
                if (filter.has(ei.getItem())) {
                    ret.add(new BlockPos(entity));
                }
            }
        }
        return ret;
    }

    @Override
    public void storeBlocks(int quantity, BlockOptionalMetaLookup filter) {
        // TODO Auto-generated method stub
        this.desiredQuantity = quantity;
        this.filter = filter;
        this.state = StoreState.STORING;
    }

    @Override
    public void storeBlocksByName(int quantity, String... blocks) {
        // TODO Auto-generated method stub
        this.desiredQuantity = quantity;
        this.filter = new BlockOptionalMetaLookup(blocks);
        this.state = StoreState.STORING;
    }

    // Checks the settings if we can use chests to store excess inventory
    private boolean canUseShulkers() {
        return Baritone.settings().storeExcessInShulkers.value;
    }

    // Checks the settings if we can use chests to store excess inventory
    private boolean canUseChests() {
        return Baritone.settings().storeExcessInChests.value;
    }

    // Checks the settings if we can just yeet the excess inventory
    private boolean canDiscard() {
        return false;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        StoreState nextState = this.state;
        // If we're storing
        if (this.state == StoreState.STORING) {
            // TODO calculate
            if (!Baritone.settings().storeExcessInventory.value) {
                logDirect("storeExcessInventory is not on");
                nextState = StoreState.DONE;
            }
            nextState = StoreState.CHECK_FOR_SHULKER; // check if we can use shulkers
            this.desiredQuantity = 5;
        } else if (this.desiredQuantity == 0) { // if we've hit our goal and we're not in our store state
            nextState = StoreState.DONE;
        } else if (this.state == StoreState.CHECK_FOR_SHULKER) {
            nextState = StoreState.FIND_CHEST;
            boolean haveShulker = isShulkerInInventory();
            if (canUseShulkers() && haveShulker) {
                // Put stuff into the shulkers
                logDebug("Put stuff in shulkers");
            } else if (!haveShulker) { // check if we have any shulkers in our inventory
                logDebug("StoreExcessInventory - we don't have any shulkers");

            }
        } else if (this.state == StoreState.FIND_CHEST) {
            nextState = StoreState.DISCARDING;
            if (canUseChests()) { // if we can use chests, look for a place to put a chest
                // TODO find a chest in our remembered inventories
            }
        } else if (this.state == StoreState.DISCARDING) {
            nextState = StoreState.DONE;
            if (canDiscard()) {
                // Throw away what we need to get rid of
            }
        }

        // If we just finished
        if (nextState == StoreState.DONE) {
            logDirect("storeExcessInventory- Done with left: " + this.desiredQuantity);
        }
        logDirect("storeExcessInventory- " + this.state + " => " + nextState);
        this.state = nextState;
        return new PathingCommand(null, PathingCommandType.DEFER); // cede to other process
    }

    @Override
    public void cancel() {
        if (this.state == StoreState.IDLE)
            return; // No need to cancel
        logDebug("CANCEL");
        onLostControl();

    }
}
