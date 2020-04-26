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
import baritone.pathing.movement.MovementState;
import baritone.utils.BaritoneProcessHelper;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.nbt.NBTTypes;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import java.util.*;
import java.util.stream.Collectors;

// Split this into a store and a obtain process?
// How do you prevent them from competing?

/**
 * Stores blocks of a certain type if your inventory becomes full It will attempt to store excess throwaway blocks, as
 * well as extra storage blocks If it cannot use shulker chests and it cannot use chests, it will just throw away the
 * acceptableThrowawayItems but will try to keep at least one stack
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

    private int tickCount = 0; // this is incremented every tick

    private int activeTicker = 0; // this is incremented on every active call

    private BlockPos shulkerPlace = null;

    private enum StoreState {
        IDLE, FULL, // We are full, after DELAY_TICKS, go to STORING
        STORING, // we need to decide how to store
        CHECK_FOR_SHULKER_BOX, // Check for shulker in our inventory
        PLACE_SHULKER_BOX, // Place the shulker in the world
        OPEN_SHULKER_BOX, // Open the shulker we placed
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

    private void resetTickers() {
        this.activeTicker = 0;
        this.tickCount = 0;
    }

    private void activeTickerIncrement() {
        this.activeTicker += 1;
    }

    private boolean activeTickerExpired() {
        return this.activeTicker >= DELAY_TICKS;
    }

    // ---------------------------------------------
    // INVENTORY MANAGEMENT FUNCTIONALITY
    // ---------------------------------------------
    private boolean isInventoryFull() {
        return ctx.player().inventory.getFirstEmptyStack() == -1;
    }

    private void swapWithHotBar(int inInventory, int inHotbar) {
        ctx.playerController().windowClick(ctx.player().container.windowId,
                inInventory < 9 ? inInventory + 36 : inInventory, inHotbar, ClickType.SWAP, ctx.player());
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
            }
        }
        System.out.println("Total: " + totalCount + ", trash: " + biggestStackOfTrash);
        return totalCount - biggestStackOfTrash;
    }

    /**
     * Used to open a chest or placed shulker box
     * @return true when we finished, false when we still aren't done
     */
    private boolean rightClickOpenChest(BlockPos pos) {
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
            if (this.tickCount % 20 == 0 && this.tickCount != 0) {
                System.out.println("Right click timed out");
                baritone.getInputOverrideHandler().clearAllKeys();
                return true;
            }
            return false; // trying to right click, will do it next tick or so
        }
        System.out.println("Arrived but failed to right click open");
        return true;
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

    /**
     * Tries to find a shulker to place
     */
    private boolean tryToPlaceShulkerChest() {
        List<Integer> slots = getSlotsWithShulkers();
        if (slots.size() == 0) {
            System.out.println("No Shulkers to place");
            return false;
        }
        int bestSlot = -1;
        int mostSpace = 0;
        for (Integer slot : slots) { // try and find the best shulker to place
            ItemStack stack = ctx.player().inventory.mainInventory.get(slot);
            int itemSpace = getEmptySpaceInShulkerBox(stack.getTag());
            System.out.println("STACK: " + stack + " @" + slot + " =" + itemSpace);

            if (itemSpace > mostSpace) {
                mostSpace = itemSpace;
                bestSlot = slot;
            }

        }
        if (bestSlot == -1)
            return false;
        // switch to the box we selected
        System.out.println("Hotbar Slot: " + bestSlot);
        if (!PlayerInventory.isHotbar(bestSlot)) {
            swapWithHotBar(bestSlot, 0);
            bestSlot = 0;
        }
        ctx.player().inventory.currentItem = bestSlot;

        System.out.println("Hotbar Slot: " + bestSlot);
        this.shulkerPlace = findPlaceToPutShulkerBox();
        if (this.shulkerPlace == null)
            return false;
        System.out.println("Shulker box @" + this.shulkerPlace);
        // TODO: Try to place it where we can get to it
        return true;
    }

    private BlockPos findPlaceToPutShulkerBox() {
        BlockPos player_pos = ctx.player().getPosition();
        BlockPos player_lower = player_pos.down();

        BlockPos places[] = {
                // upper half
                player_pos.add(1, 0, 1), player_pos.add(1, 0, 0), player_pos.add(0, 0, 1), player_pos.add(-1, 0, 0),
                player_pos.add(0, 0, -1), player_pos.add(-1, 0, 1), player_pos.add(1, 0, -1), player_pos.add(-1, 0, -1),
                // lower half
                player_lower.add(1, 0, 1), player_lower.add(1, 0, 0), player_lower.add(0, 0, 1),
                player_lower.add(-1, 0, 0), player_lower.add(0, 0, -1), player_lower.add(-1, 0, 1),
                player_lower.add(1, 0, -1), player_lower.add(-1, 0, -1) };
        BlockStateInterface bsi = baritone.bsi;
        for (BlockPos place : places) {
            BlockState upper = bsi.get0(place.up());
            if (bsi.get0(place).isAir() && (upper.isTransparent() || upper.isAir())) // Make sure we can place the thing
                                                                                     // there and we can open it later
                return place;
        }
        System.out.println("We failed to find a spot to place the shulker box");
        logDebug("We failed to find a spot to place the shulker box");
        return null;

    }

    /**
     * Returns the number of empty slots in a shulker
     */
    private int getEmptySpaceInShulkerBox(CompoundNBT data) {
        System.out.println("DATA: " + data);
        int defaultSize = 27;
        if (data == null)
            return defaultSize;
        CompoundNBT blockEntityTag = data.getCompound("BlockEntityTag");
        if (blockEntityTag == null)
            return defaultSize;
        ListNBT items = blockEntityTag.getList("Items", 10);
        if (items == null)
            return defaultSize;
        // TODO: figure out how much we can fit of what we currently have
        return defaultSize - items.size();
    }

    /**
     * Scans for dropped items items
     */
    public List<BlockPos> droppedShulkerBoxScan(Set<Item> filter) {
        List<BlockPos> ret = new ArrayList<>();
        for (Entity entity : ((ClientWorld) ctx.world()).getAllEntities()) {
            if (entity instanceof ItemEntity) {
                ItemEntity ei = (ItemEntity) entity;
                if (ei.getItem().getItem().equals(Items.SHULKER_BOX))
                    ret.add(new BlockPos(entity));
            }
        }
        // We found
        return ret;
    }

    // Checks the settings if we can use chests to store excess inventory
    private boolean canUseShulkers() {
        return Baritone.settings().storeExcessInShulkers.value;
    }

    /**
     * Returns a list with the inventory slots that have shulkers in them
     */
    private List<Integer> getSlotsWithShulkers() {
        List<Integer> list = new ArrayList<>();
        int mainInventorySize = ctx.player().inventory.mainInventory.size();
        for (int i = 0; i < mainInventorySize; i++) {
            ItemStack stack = ctx.player().inventory.mainInventory.get(i);
            if (stack.getItem() == Items.SHULKER_BOX) {
                list.add(i);
            }
        }
        return list;
    }

    // ---------------------------------------------
    // Management functions
    // ---------------------------------------------

    @Override
    public void storeBlocks(int quantity, BlockOptionalMetaLookup filter) {
        this.desiredQuantity = quantity;
        // TODO: fix filter
        // this.filter = filter;
        this.state = StoreState.STORING;
    }

    @Override
    public void storeBlocksByName(int quantity, String... blocks) {
        this.desiredQuantity = quantity;
        // TODO: fix filter
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
        resetTickers();
    }

    @Override
    public String displayName0() {
        return "Inventory Store" + this.state.toString();
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        StoreState nextState = this.state;
        this.activeTicker += 1; // make sure to add to the ticker
        // If we're storing
        if (this.state == StoreState.STORING) {
            // TODO calculate
            nextState = StoreState.DONE;
            if (!Baritone.settings().storeExcessInventory.value) {
                logDirect("storeExcessInventory is not on");
            }
            else {
                // figure out how much we can get rid of
                this.setupFilter();
                this.desiredQuantity = numberThingsAvailableToStore();
                if (this.desiredQuantity > 0)
                    nextState = StoreState.CHECK_FOR_SHULKER_BOX;
            }
        }
        else if (this.desiredQuantity == 0) { // if we've hit our goal and we're not in our store state
            nextState = StoreState.DONE;
            // -----------------------
            // SHULKER STORAGE
        }
        else if (this.state == StoreState.CHECK_FOR_SHULKER_BOX) {
            nextState = StoreState.FIND_CHEST;
            if (canUseShulkers()) { // Put stuff into the shulkers
                boolean result = tryToPlaceShulkerChest();
                logDebug("Put stuff in shulkers");
                if (result)
                    nextState = StoreState.PLACE_SHULKER_BOX;
            }
            else {
                System.out.println("Can't use shulkers");
            }
        }
        else if (this.state == StoreState.PLACE_SHULKER_BOX) {
            baritone.getInputOverrideHandler().clearAllKeys();
            Optional<Rotation> shulker_reachable = RotationUtils.reachable(ctx.player(), this.shulkerPlace,
                    ctx.playerController().getBlockReachDistance());
            Optional<Rotation> under_reachable = RotationUtils.reachable(ctx.player(), this.shulkerPlace.down(),
                    ctx.playerController().getBlockReachDistance());
            // TODO: figure out how to make this better so it doesn't suck
            if (shulker_reachable.isPresent()) {
                // Look at it
                baritone.getLookBehavior().updateTarget(shulker_reachable.get(), true);

                if (this.shulkerPlace.equals(ctx.getSelectedBlock().orElse(null))) {
                    // We did it!
                    nextState = StoreState.OPEN_SHULKER_BOX;
                }
            }
            else if (under_reachable.isPresent()) {
                // Look at it
                baritone.getLookBehavior().updateTarget(under_reachable.get(), true);
                if (this.shulkerPlace.down().equals(ctx.getSelectedBlock().orElse(null))) { // wait for us to actually
                                                                                            // look at the block
                    baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true); // firmly grasp it
                    return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL); // cede to other process
                }
                else {
                    System.out.println("We aren't looking at it yet");
                }
                // Check if we actually placed a block
            }
            else if (this.tickCount % 100 == 0) {
                // If we happy upon an unlucky start
                nextState = StoreState.DONE;
            }
            else {
                System.out.println("I can't reach this block");
            }
            if (nextState == StoreState.PLACE_SHULKER_BOX)
                return new PathingCommand(new GoalGetToBlock(this.shulkerPlace), PathingCommandType.SET_GOAL_AND_PATH);

        }
        else if (this.state == StoreState.OPEN_SHULKER_BOX) {
            if (this.rightClickOpenChest(this.shulkerPlace))
                nextState = StoreState.STORE_IN_SHULKER;
        }
        else if (this.state == StoreState.STORE_IN_SHULKER) {

        }
        else if (this.state == StoreState.MINE_SHULKER_BOX) {
            baritone.getInputOverrideHandler().clearAllKeys();
            if (baritone.bsi.get0(this.shulkerPlace).isAir()) {
                nextState = StoreState.COLLECT_SHULKER_BOX;
            }
            else {
                // Look at it
                // TODO- this code should be in a function
                Optional<Rotation> shulker_reachable = RotationUtils.reachable(ctx.player(), this.shulkerPlace, ctx.playerController().getBlockReachDistance());
                if (shulker_reachable.isPresent()) {
                    baritone.getLookBehavior().updateTarget(shulker_reachable.get(), true);
                }
                // check if we're looking at it
                if (this.shulkerPlace.equals(ctx.getSelectedBlock().orElse(null))) {
                    baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true); // firmly grasp it
                }
            }
        }
        // -----------------------
        // CHEST STORAGE
        else if (this.state == StoreState.FIND_CHEST) {
            nextState = StoreState.DISCARDING;
            if (canUseChests()) { // if we can use chests, look for a place to put a chest
                // TODO find a chest in our remembered inventories
            }
        }
        else if (this.state == StoreState.DISCARDING) {
            nextState = StoreState.DONE;
            if (canDiscard()) {
                // Throw away what we need to get rid of
            }
        }
        else {
            System.out.println("UNKNOWN state: " + this.state);
            nextState = StoreState.DONE;
        }

        // If we just finished
        if (nextState == StoreState.DONE) {
            logDirect("storeExcessInventory- Done with left: " + this.desiredQuantity);
        }
        logDirect("storeExcessInventory- " + this.state + " => " + nextState);
        this.state = nextState;
        return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE); // cede to other process
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
            activeTickerIncrement();
            if (activeTickerExpired()) {
                this.state = StoreState.IDLE;
                resetTickers();
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
            resetTickers();
            return false;
        }
        // Add to our tick count
        if (!activeTickerExpired()) {
            activeTickerIncrement();
            return false;
        }

        // Reset out tick count
        resetTickers();
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
        }
        else {
            this.state = StoreState.FULL;
            return false;
        }
    }
}
