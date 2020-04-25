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
import net.minecraft.item.Items;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.stream.Collectors;

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

    private class ItemFilter {
        public final Item i;
        public final boolean garbage;

        public ItemFilter(Item i, boolean garbage) {
            this.i = i;
            this.garbage = garbage;
        }

        @Override
        public int hashCode() {
            return this.i.hashCode();
        }

        @Override
        public String toString() {
            return "(" + this.i.toString() + ", " + this.garbage + ")";
        }
    }

    private List<ItemFilter> filter = new ArrayList<>(); // TODO make this an item filter, not a block filter
    final int DELAY_TICKS = 50; // how many ticks we wait after the inventory is full before we start doing
                                // something
    private int desiredQuantity = -1; // -1 means no desire, 0 means store as much as you can, >0 means store up to
                                      // that much
    private int tickCount = 0;

    private enum StoreState {
        IDLE, FULL, // We are full, after DELAY_TICKS, go to STORING
        STORING, // we need to decide how to store
        CHECK_FOR_SHULKER_BOX, // Check for shulker in our inventory
        PLACE_SHULKER_BOX, // Place the shulker in the world
        STORE_IN_SHULKER, // Open the shulker and store in it
        MINE_SHULKER_BOX, // Now we need to mine the shulker that we placed
        COLLECT_SHULKER_BOX, // Now we need to collect the tile that dropped
        FIND_CHEST, // find a stored inventory that has space
        GO_TO_CHEST, // go to chest
        AT_CHEST, // Once we're at the chest, place the stuff inside
        DISCARDING, // Throw away the items until we can't anymore
        DONE // we finished
    }

    private StoreState state = StoreState.IDLE;

    public InventoryStoreProcess(Baritone baritone) {
        super(baritone);
    }

    // ---------------------------------------------
    // TICKER FUNCTIONALITY
    // ---------------------------------------------

    private void resetTicker() {
        this.tickCount = 0;
    }

    private void incrementTicker() {
        this.tickCount += 1;
    }

    private boolean tickerExpired() {
        return this.tickCount >= DELAY_TICKS;
    }

    // ---------------------------------------------
    // INVENTORY MANAGEMENT FUNCTIONALITY
    // ---------------------------------------------
    private boolean isInventoryFull() {
        return ctx.player().inventory.getFirstEmptyStack() == -1;
    }

    // Based on filter, how many items do we have in the inventory to store?
    private int numberThingsAvailableToStore() {
        NonNullList<ItemStack> inv = ctx.player().inventory.mainInventory;
        int totalCount = 0;
        int biggestStackOfTrash = 0; // keep track of the biggest stack of trash we have
        for (ItemFilter item_filter : this.filter) {
            // Check if we have this item
            for (ItemStack item : inv) {
                if (item == null)
                    continue;
                if (item.getItem().equals(item_filter.i)) {
                    totalCount += item.getCount();
                    if (item_filter.garbage && biggestStackOfTrash < item.getCount())
                        biggestStackOfTrash = item.getCount();
                }
                System.out.println("numberThingsAvailableToStore " + item_filter.toString() + " vs " + item.toString()
                        + " stack: " + biggestStackOfTrash);
            }
        }
        System.out.println("Total: " + totalCount + ", trash: " + biggestStackOfTrash);
        return totalCount - biggestStackOfTrash;
    }

    /**
     * Scans for dropped items items
     */
    public List<BlockPos> droppedItemsScan(Set<Item> filter) {
        List<BlockPos> ret = new ArrayList<>();
        for (Entity entity : ((ClientWorld) ctx.world()).getAllEntities()) {
            if (entity instanceof ItemEntity) {
                ItemEntity ei = (ItemEntity) entity;
                if (filter.contains(ei.getItem().getItem()))
                    ret.add(new BlockPos(entity));
            }
        }
        return ret;
    }

    // Checks the settings if we can use chests to store excess inventory
    private boolean canUseChests() {
        return Baritone.settings().storeExcessInChests.value;
    }

    // Checks the settings if we can just yeet the excess inventory
    private boolean canDiscard() {
        return false;
    }

    /**
     * This code figures out what we can throw away
     */
    private void setupFilter() {
        this.desiredQuantity = 0;
        List<Item> throwAwayItems = Baritone.settings().acceptableThrowawayItems.value;
        List<Item> wantedItems = Baritone.settings().itemsToStore.value;
        this.filter.clear(); // set the filter up so that it will
        // look for items in the inventory
        Set<ItemFilter> filteredItems = new HashSet<>();
        // Add the wanted items and the throwaway items to the filter list
        for (Item item : throwAwayItems)
            filteredItems.add(new ItemFilter(item, true));
        for (Item item : wantedItems)
            filteredItems.add(new ItemFilter(item, false));
        this.filter.addAll(filteredItems);
    }

    // ---------------------------------------------
    // SHULKER FUNCTIONS
    // ---------------------------------------------

    private boolean tryToStoreInShulkers() {
        List<Integer> slots = getSlotsWithShulkers();
        if (slots.size() == 0)
            return false;
        for (Integer slot : slots) {
            // Check if shulker is empty
        }
        return false;
    }

    // Checks the settings if we can use chests to store excess inventory
    private boolean canUseShulkers() {
        return Baritone.settings().storeExcessInShulkers.value;
    }

    // Checks whether we have any shulkers in our inventory
    private boolean isShulkerBoxInInventory() {
        return getSlotsWithShulkers().size() > 0;
    }

    /**
     * Returns a list with the inventory slots that have shulkers in them
     */
    private List<Integer> getSlotsWithShulkers() {
        List<Integer> list = new ArrayList<>();
        for (ItemStack stack : ctx.player().inventory.mainInventory) {
            System.out.println(stack.toString());
            System.out.println(stack.getItem());
            if (stack.getItem() == Items.SHULKER_BOX) {
                list.add(0); // TODO add this
            }
        }
        return list;
    }

    // ---------------------------------------------
    // Management functions
    // ---------------------------------------------

    @Override
    public void storeBlocks(int quantity, BlockOptionalMetaLookup filter) {
        // TODO Auto-generated method stub
        this.desiredQuantity = quantity;
        // this.filter = filter;
        this.state = StoreState.STORING;
    }

    @Override
    public void storeBlocksByName(int quantity, String... blocks) {
        // TODO Auto-generated method stub
        this.desiredQuantity = quantity;
        // this.filter = new BlockOptionalMetaLookup(blocks);
        this.state = StoreState.STORING;
    }

    @Override
    public void cancel() {
        if (this.state == StoreState.IDLE)
            return; // No need to cancel
        logDebug("CANCEL");
        onLostControl();
    }

    @Override
    public double priority() {
        return 2; // sort of arbitrary but it seems like a good amount?
    }

    @Override
    public void onLostControl() {
        this.state = StoreState.IDLE; // we're gonna drop back to idle
        resetTicker();
    }

    @Override
    public String displayName0() {
        // TODO figure out if we're storing or obtaining
        return "Inventory Store" + filter.toString();
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        StoreState nextState = this.state;
        // If we're storing
        if (this.state == StoreState.STORING) {
            // TODO calculate
            nextState = StoreState.DONE;
            if (!Baritone.settings().storeExcessInventory.value) {
                logDirect("storeExcessInventory is not on");
            } else {
                // figure out how much we can get rid of
                this.setupFilter();
                this.desiredQuantity = numberThingsAvailableToStore();
                if (this.desiredQuantity > 0)
                    nextState = StoreState.CHECK_FOR_SHULKER_BOX;
            }

        } else if (this.desiredQuantity == 0) { // if we've hit our goal and we're not in our store state
            nextState = StoreState.DONE;
        } else if (this.state == StoreState.CHECK_FOR_SHULKER_BOX) {
            nextState = StoreState.FIND_CHEST;
            boolean haveShulker = isShulkerBoxInInventory();
            if (canUseShulkers() && haveShulker) { // Put stuff into the shulkers
                boolean result = tryToStoreInShulkers();
                logDebug("Put stuff in shulkers");
                if (result)
                    nextState = StoreState.CHECK_FOR_SHULKER_BOX;
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
            incrementTicker();
            if (tickerExpired()) {
                this.state = StoreState.IDLE;
                resetTicker();
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
            resetTicker();
            return false;
        }
        // Add to our tick count
        if (!tickerExpired()) {
            incrementTicker();
            return false;
        }

        // Reset out tick count
        resetTicker();
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
    }
}
