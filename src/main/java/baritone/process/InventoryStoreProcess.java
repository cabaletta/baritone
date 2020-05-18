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
 * 
 * Author: Matthew Carlson (@matthewfcarlson)
 */

package baritone.process;

import baritone.Baritone;
import baritone.api.pathing.goals.*;
import baritone.api.process.IInventoryStoreProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.*;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.MovementHelper;
import baritone.utils.BaritoneProcessHelper;
import baritone.utils.inventory.ChestHelper;
import baritone.utils.inventory.InventoryHelper;
import baritone.utils.inventory.ItemFilter;
import baritone.utils.inventory.ShulkerHelper;
import net.minecraft.block.*;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import java.util.*;
import java.util.stream.Collectors;

// This file is crazy long and needs to be 

/**
 * Stores blocks of a certain type if your inventory becomes full It will attempt to store excess throwaway blocks, as
 * well as extra storage blocks If it cannot use shulker chests and it cannot use chests, it will just throw away the
 * acceptableThrowawayItems but will try to keep at least one stack
 * 
 * @author matthewfcarlson
 */
public final class InventoryStoreProcess extends BaritoneProcessHelper implements IInventoryStoreProcess {

    private List<ItemFilter> filter = new ArrayList<>();

    final int DELAY_TICKS = 50; // how many ticks we wait after the inventory is full before we start doing
                                // something

    private int desiredQuantity = -1; // -1 means no desire, 0 means store as much as you can, >0 means store up to
                                      // that much

    private int tickCount = 0; // this is incremented every tick

    private int activeTicker = 0; // this is incremented on every active call

    private BlockPos shulkerPlace = null;

    private InventoryHelper invHelper = null;

    private ChestHelper chestHelper = null;

    private ShulkerHelper shulkerHelper = null;

    private enum StoreState {
        IDLE, FULL, // We are full, after DELAY_TICKS, go to STORING
        CONDENSE_INVENTORY, // Try to condense our inventory
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
        this.invHelper = new InventoryHelper(ctx);
        this.chestHelper = new ChestHelper(ctx, baritone);
        this.shulkerHelper = new ShulkerHelper(ctx, baritone);
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

    // Checks the settings if we can use chests to store excess inventory
    private boolean canUseChests() {
        return false;
        // by default this is disabled until finished
        // return Baritone.settings().storeExcessInChests.value;
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
        this.filter.clear();
        List<Item> throwAwayItems = Baritone.settings().acceptableThrowawayItems.value;
        List<Item> wantedItems = Baritone.settings().itemsToStore.value;
        List<Item> minedItems = baritone.getMineProcess().getCurrentItems();
        System.out.print("Including the following mined items:");
        for (Item i: minedItems) System.out.print(i);
        System.out.println("");
         // set the filter up so that it will
        // look for items in the inventory
        Set<ItemFilter> filteredItems = new HashSet<>();
        // Add the wanted items and the throwaway items to the filter list
        for (Item item : minedItems)
            filteredItems.add(new ItemFilter(item, 2)); // mined items get highest priority
        for (Item item : throwAwayItems)
            filteredItems.add(new ItemFilter(item, 1));
        for (Item item : wantedItems)
            filteredItems.add(new ItemFilter(item, 0));
        this.filter.addAll(filteredItems);
    }

    // ---------------------------------------------
    // SHULKER FUNCTIONS
    // ---------------------------------------------

    /**
     * Tries to find a shulker to place
     */
    private boolean tryToPlaceShulkerChest() {
        int bestSlot = -1;
        int mostSpace = 0;
        int invSize = ctx.player().inventory.mainInventory.size();
        for (int slot = 0; slot < invSize; slot++) { // try and find the best shulker to place
            ItemStack stack = ctx.player().inventory.mainInventory.get(slot);
            if (!ShulkerHelper.isShulkerBox(stack))
                continue;
            int itemSpace = ShulkerHelper.getEmptySpaceInShulkerBox(stack);
            System.out.println("STACK: " + stack + " @" + slot + " =" + itemSpace);

            if (itemSpace > mostSpace) {
                mostSpace = itemSpace;
                bestSlot = slot;
            }
        }
        if (bestSlot == -1) {
            System.out.println("No Shulkers to place");
            return false;
        }
        // Find a new place to place it
        ArrayList<Integer> candidates = new ArrayList<>(); // They use 0 and 8
        candidates.add(0);
        candidates.add(8);

        OptionalInt newSlot = baritone.getInventoryBehavior().attemptToPutOnHotbar(bestSlot, candidates::contains);

        if (newSlot.isPresent()) {
            int newSlotInt = newSlot.getAsInt();
            System.out.println("Switching from " + ctx.player().inventory.currentItem + " to " + newSlotInt);
            ctx.player().inventory.currentItem = newSlotInt;
        }
        else if (ctx.player().getHeldItemOffhand().isEmpty()) {
            baritone.getInventoryBehavior().attemptToPlaceInOffhand(bestSlot);
        }
        else {
            System.out.println("I don't know what to do here");
        }
        //
        if (!this.shulkerHelper.isCurrentHeldItemShulkerBox()) {
            System.out.println("Failed to get shulker in our hotbar");
            return false;
        }
        // Now figure out where to place the shulker
        this.shulkerPlace = shulkerHelper.findPlaceToPutShulkerBox();
        if (this.shulkerPlace == null)
            return false;
        System.out.println("Will try to put Shulker box @" + this.shulkerPlace);
        return true;
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

    // ---------------------------------------------
    // Process Management functions
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

    private int calculateDesiredInventory() {
        // First we create a filter
        List<ItemFilter> nonGarbage = this.filter.stream().filter(x -> x.isGarbage()).collect(Collectors.toList());
        List<ItemFilter> justGarbage = this.filter.stream().filter(x -> !x.isGarbage()).collect(Collectors.toList());
        int totalCount = invHelper.numberItemsInInventory(nonGarbage);
        int garbageCount = invHelper.numberItemsInInventory(justGarbage);
        int garbageStackCount = invHelper.getSizeOfLargestStackOfItemsInInventory(justGarbage);
        return totalCount + garbageCount - garbageStackCount;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        StoreState nextState = this.state;
        this.tickCount += 1; // make sure to add to the ticker
        // -----------------------
        // CONDENSING
        if (this.state == StoreState.CONDENSE_INVENTORY) {
            this.desiredQuantity = 0;
            if (!this.invHelper.attemptToCondense())
                nextState = StoreState.STORING;
        }
        // -----------------------
        // Storing
        else if (this.state == StoreState.STORING) {

            nextState = StoreState.DONE;
            if (!invHelper.isInventoryFull() && this.desiredQuantity == 0) {
                // we don't need to do anything
                System.out.println("We're done here!");
            }
            else if (!Baritone.settings().storeExcessInventory.value) {
                logDirect("storeExcessInventory is not on");
            }
            else {
                // figure out how much we can get rid of
                this.setupFilter();
                this.desiredQuantity = calculateDesiredInventory();
                if (this.desiredQuantity > 0)
                    nextState = StoreState.CHECK_FOR_SHULKER_BOX;
            }
        }
        else if (this.desiredQuantity == 0) { // if we've hit our goal and we're not in our store state
            nextState = StoreState.DONE;
        }
        // -----------------------
        // SHULKER STORAGE
        else if (this.state == StoreState.CHECK_FOR_SHULKER_BOX) {
            nextState = StoreState.CHECK_FOR_CHEST;
            if (canUseShulkers()) { // Put stuff into the shulkers
                boolean result = tryToPlaceShulkerChest();
                System.out.println("Put stuff in shulkers");
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
            BlockState state = baritone.bsi.get0(shulkerPlace);
            boolean placedShulker = !state.isAir() && ShulkerHelper.isShulkerBox(state.getBlock().asItem());
            // if we haven't placed the shulker
            if (!placedShulker && !ShulkerHelper.isShulkerBox(ctx.player().inventory.getCurrentItem())) {
                logDirect("We failed to put the shulker in our hand");
                nextState = StoreState.CHECK_FOR_CHEST;
            }
            if (shulker_reachable.isPresent()) {
                // Look at it
                baritone.getLookBehavior().updateTarget(shulker_reachable.get(), true);
                // If the place is filled and it isn't a shulker box
                if (!placedShulker) {
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
            if (chestHelper.rightClickOpenChest(this.shulkerPlace))
                nextState = StoreState.STORE_IN_SHULKER;
        }
        else if (this.state == StoreState.STORE_IN_SHULKER) {
            ClientPlayerEntity player = ctx.player();
            Container openContainer = player.openContainer;
            if (openContainer == player.container) {
                System.out.println("Finished putting stuff in the shulker");
                nextState = StoreState.MINE_SHULKER_BOX;
            }
            else {
                // if we can't place anything else in the chest
                int deposited = chestHelper.transferItemsToOpenChest(this.filter, this.desiredQuantity, false);
                this.desiredQuantity -= deposited;
                System.out.println("Deposited " + deposited);
                if (deposited == 0)
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
                if (this.shulkerPlace == null || shulkerBoxes.size() == 0)
                    nextState = StoreState.STORE_IN_SHULKER;
                else if (shulkerBoxes.size() > 0)
                    this.shulkerPlace = shulkerBoxes.get(0);
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

        // This doesn't apply if we're creative
        if (ctx.player().isCreative())
            return false;

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
        if (!this.invHelper.isInventoryFull()) {
            this.state = StoreState.IDLE;
            return false;
        }
        // If we're in the full state, it's time to start storing
        if (this.state == StoreState.FULL) {
            this.state = StoreState.CONDENSE_INVENTORY; // set our state to STORING
            return true;
        }
        else {
            this.state = StoreState.FULL;
            return false;
        }
    }
}
