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
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.inventory.ShulkerBoxScreen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.inventory.container.Slot;
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

    private List<ItemFilter> filter = new ArrayList<>();

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
        CHECK_FOR_CHEST, // find a stored inventory that has space
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

    /**
     * This assumes that the chest is currently open
     */
    private boolean transferItemsToOpenChest() {
        ClientPlayerEntity player = ctx.player();
        if (ctx.player().openContainer instanceof PlayerContainer) {
            // We opened our own inventory!
            System.out.println("We don't have a container open");
        }
        boolean did_transfer_items = false;

        int numberChestSlots = 27; // TODO: figure out what are slots of our inventory and what are slots in the chest
        // Go through each of the slot in the chest
        // TODO: check if we can do this
        NonNullList<ItemStack> inv = player.inventory.mainInventory;
        for (int j = 0; j < inv.size(); j++) {
            ItemStack stack = inv.get(j);
            if (stack.isEmpty())
                continue;
            // System.out.println("Compared to " + j + " = " + stack);
            if (!isNonGarbageItem(stack.getItem()))
                continue;
            int stackSize = stack.getCount();
            System.out.println("Transferring goods " + j + ": " + stack);
            int slotId = j;
            // Slot chest_slot = player.openContainer.getSlot(slotId);
            // Check to make sure we are doing the slot we think we are
            // Click on the thing in our inventory?
            ItemStack outstack = ctx.playerController().windowClick(ctx.player().container.windowId, slotId, 1,
                    ClickType.QUICK_MOVE, ctx.player());
            System.out.println("Tried to move " + outstack);
            this.desiredQuantity -= stackSize;

            did_transfer_items = true;
            break;
        }
        // Now we try the garbage
        for (int j = 0; j < inv.size() && !did_transfer_items; j++) {
            ItemStack stack = inv.get(j);
            if (stack.isEmpty())
                continue;
            // System.out.println("Compared to " + j + " = " + stack);
            if (!isGarbageItem(stack.getItem()))
                continue;
            this.desiredQuantity -= stack.getCount();
            System.out.println("Transferring garbage " + j + ":" + stack);
            // how do I figure out what this is
            int slotId = j;
            // Slot chest_slot = player.openContainer.getSlot(slotId);
            // Check to make sure we are doing the slot we think we are
            // Click on the thing in our inventory?
            ItemStack outstack = ctx.playerController().windowClick(ctx.player().container.windowId, slotId, 1,
                    ClickType.QUICK_MOVE, ctx.player());
            System.out.println("Tried to move " + outstack);
            did_transfer_items = true;
            break;
        }
        // TODO: try to consolidate chest- so we can store more things next time
        if (did_transfer_items) {
            System.out.println("We should consolidate the chest");
        }
        return did_transfer_items;
    }

    /**
     * Uses the filer to check if this is garbage
     */
    private boolean isGarbageItem(Item i) {
        for (ItemFilter item_filter : this.filter) { // TODO: have a better way to check if this is in there
            if (item_filter.garbage && item_filter.i.equals(i)) { // If it's a match
                return true;
            }
        }
        return false;
    }

    /**
     * Uses the filter to check if this is a non garbage item
     */
    private boolean isNonGarbageItem(Item i) {
        for (ItemFilter item_filter : this.filter) { // TODO: have a better way to check if this is in there
            if (!item_filter.garbage && item_filter.i.equals(i)) { // If it's a match
                return true;
            }
        }
        return false;
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
        int time_attempted = 0;
        ArrayList<Integer> usefulSlots = new ArrayList<>();
        // TODO figure out what we care about?
        OptionalInt newSlot = baritone.getInventoryBehavior().attemptToPutOnHotbar(bestSlot, usefulSlots::contains);
        if (!newSlot.isPresent()) {
            System.out.println("We failed to equipt the shulker box");
            return false;
        }
        ctx.player().inventory.currentItem = newSlot.getAsInt();

        if (!isShulkerBox(ctx.player().inventory.getCurrentItem())) {
            System.out.println("Failed to get shulker");
            return false;
        }
        // Now figure out where to place the shulker
        this.shulkerPlace = findPlaceToPutShulkerBox();
        if (this.shulkerPlace == null)
            return false;
        System.out.println("Shulker box @" + this.shulkerPlace);
        // TODO: Try to place it where we can get to it
        return true;
    }

    private boolean isShulkerBox(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        return isShulkerBox(stack.getItem());
    }

    private boolean isShulkerBox(Item i) {
        return i.equals(Items.SHULKER_BOX) || i.equals(Items.BLACK_SHULKER_BOX) || i.equals(Items.BLUE_SHULKER_BOX)
                || i.equals(Items.BROWN_SHULKER_BOX) || i.equals(Items.CYAN_SHULKER_BOX)
                || i.equals(Items.GRAY_SHULKER_BOX) || i.equals(Items.GREEN_SHULKER_BOX)
                || i.equals(Items.LIME_SHULKER_BOX) || i.equals(Items.MAGENTA_SHULKER_BOX)
                || i.equals(Items.ORANGE_SHULKER_BOX) || i.equals(Items.PINK_SHULKER_BOX)
                || i.equals(Items.PURPLE_SHULKER_BOX) || i.equals(Items.RED_SHULKER_BOX)
                || i.equals(Items.WHITE_SHULKER_BOX) || i.equals(Items.YELLOW_SHULKER_BOX);
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
            BlockState lower = bsi.get0(place.down());
            if (bsi.get0(place).isAir() && (upper.isTransparent() || upper.isAir()) && lower.isSolid()) // Make sure we
                                                                                                        // can place the
                                                                                                        // thing
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
    public List<BlockPos> droppedShulkerBoxScan() {
        List<BlockPos> ret = new ArrayList<>();
        System.out.println("DroppedShulkerBoxScan");
        // TODO sort by distance from player
        for (Entity entity : ((ClientWorld) ctx.world()).getAllEntities()) {
            if (entity instanceof ItemEntity) {
                ItemEntity ei = (ItemEntity) entity;
                if (ei.getItem().getItem().equals(Items.SHULKER_BOX)) {
                    BlockPos pos = new BlockPos(entity);
                    ret.add(pos);
                }
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
        this.tickCount += 1; // make sure to add to the ticker
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
            nextState = StoreState.CHECK_FOR_CHEST;
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
            if (!isShulkerBox(ctx.player().inventory.getCurrentItem())) {
                logDirect("We failed to put the shulker in our hand");
                nextState = StoreState.CHECK_FOR_CHEST;
            }
            Optional<Rotation> shulker_reachable = RotationUtils.reachable(ctx.player(), this.shulkerPlace,
                    ctx.playerController().getBlockReachDistance());
            Optional<Rotation> under_reachable = RotationUtils.reachable(ctx.player(), this.shulkerPlace.down(),
                    ctx.playerController().getBlockReachDistance());
            // TODO: figure out how to make this better so it doesn't suck
            if (shulker_reachable.isPresent()) {
                // Look at it
                baritone.getLookBehavior().updateTarget(shulker_reachable.get(), true);
                BlockState state = baritone.bsi.get0(shulkerPlace);
                // If the place is filled and it isn't a shulker box
                if (!state.isAir() && state.getBlock() != Blocks.SHULKER_BOX) {
                    System.out.println("We messed up " + state);
                    nextState = StoreState.CHECK_FOR_SHULKER_BOX;
                }
                else if (this.shulkerPlace.equals(ctx.getSelectedBlock().orElse(null))) {
                    // We did it!
                    nextState = StoreState.OPEN_SHULKER_BOX;
                }
            }
            else if (under_reachable.isPresent()) {
                // Look at it
                baritone.getLookBehavior().updateTarget(under_reachable.get(), true);
                // If we're looking at it?
                // TODO: determine if we are looking at a top face?
                if (this.shulkerPlace.down().equals(ctx.getSelectedBlock().orElse(null))) { // wait for us to actually
                                                                                            // look at the block
                    // Place that funky shulker box white boy
                    System.out.println("Placing that block");
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
            ClientPlayerEntity player = ctx.player();
            Container openContainer = player.openContainer;
            if (openContainer == player.container) {
                System.out.println("Finished putting stuff in the shulker");
                nextState = StoreState.MINE_SHULKER_BOX;
            }
            else if (this.tickCount % 20 == 0 && this.tickCount != 0) {
                // if we can't place anything else in the chest
                if (!transferItemsToOpenChest())
                    ctx.player().closeScreen();
            }
        }
        else if (this.state == StoreState.MINE_SHULKER_BOX) {
            baritone.getInputOverrideHandler().clearAllKeys();
            if (baritone.bsi.get0(this.shulkerPlace).isAir()) {
                this.shulkerPlace = null;
                nextState = StoreState.COLLECT_SHULKER_BOX;
            }
            else {
                // Make sure we got some sort of pickaxe?
                MovementHelper.switchToBestToolFor(ctx, ctx.world().getBlockState(this.shulkerPlace));
                // TODO: switch to tool that's best for the job
                Optional<Rotation> shulker_reachable = RotationUtils.reachable(ctx.player(), this.shulkerPlace,
                        ctx.playerController().getBlockReachDistance());
                if (shulker_reachable.isPresent()) {
                    baritone.getLookBehavior().updateTarget(shulker_reachable.get(), true);
                }
                // check if we're looking at it
                if (this.shulkerPlace.equals(ctx.getSelectedBlock().orElse(null))) {
                    baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, true); // firmly punch it
                }
            }
        }
        else if (this.state == StoreState.COLLECT_SHULKER_BOX) {
            if (this.shulkerPlace == null || this.tickCount % 10 == 0) {
                List<BlockPos> shulkerBoxes = droppedShulkerBoxScan();
                // TODO find the closest
                if (shulkerBoxes.size() > 0)
                    this.shulkerPlace = shulkerBoxes.get(0);
                else {
                    // We found it? Go back to the top to use a different shulker?
                    System.out.println("We finished");
                    nextState = StoreState.STORE_IN_SHULKER;
                }
                if (this.shulkerPlace == null)
                    nextState = StoreState.DONE;
            }
            else {
                return new PathingCommand(new GoalBlock(this.shulkerPlace), PathingCommandType.SET_GOAL_AND_PATH);
            }
        }
        // -----------------------
        // CHEST STORAGE
        else if (this.state == StoreState.CHECK_FOR_CHEST) {
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
        if (this.state != nextState)
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
