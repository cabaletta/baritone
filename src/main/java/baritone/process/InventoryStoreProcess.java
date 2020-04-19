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
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.stream.Collectors;

import static baritone.api.pathing.movement.ActionCosts.COST_INF;

// Split this into a store and a obtain process?
// How do you prevent them from competing?

/**
 * Stores blocks of a certain type if your inventory becomes full
 * It will attempt to store excess throwaway blocks, as well as extra storage blocks
 * @author matthewfcarlson
 */
public final class InventoryStoreProcess extends BaritoneProcessHelper implements IInventoryStoreProcess {

    private BlockOptionalMetaLookup filter;
    private int desiredQuantity = -1; // -1 means no desire, 0 means store as much as you can, >0 means store up to that much
    private int tickCount;


    public InventoryStoreProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public boolean isActive() {
        if (ctx.player() == null || ctx.world() == null) {
            return false;
        }
        if (!Baritone.settings().storeExcessInventory.value) { // TODO also check obtain
            return false;
        }
        
        // check if our inventory is full
        if (!Baritone.settings().allowInventory.value) {
            logDirect("storeExcessInventory cannot be used with allowInventory false");
            Baritone.settings().storeExcessInventory.value = false;
            return false;
        }

        if (desiredQuantity >= 0) {
            return true;
        }

        if (ctx.player().openContainer != ctx.player().container) {
            // we have a crafting table or a chest or something open
            return false;
        }

        // If we don't have any empty slots left
        if (ctx.player().inventory.getFirstEmptyStack() == -1) {
            logDirect("storeExcessInventory- we don't have a free slot");
            
            this.desiredQuantity = 0;
            List<Item> throwAwayItems = Baritone.settings().acceptableThrowawayItems.value;
            List<Item> wantedItems = Baritone.settings().itemsToSore.value;
            // set the filter up so that it will look for items in the inventory 
            this.filter = new BlockOptionalMetaLookup("minecraft:dirt");
            int storableCount = numberThingsAvailableToStore();
            if (storableCount > 0){
                // We need to figure out 
                logDirect("storeExcessInventory- " + storableCount + " items");
                desiredQuantity = storableCount;
                return true;
            }
        }
        return false;
    }

    // Based on filter, how many items do we have in the inventory to store?
    private int numberThingsAvailableToStore() {
        
        return 0;
    }

    @Override
    public double priority() {
        return 2; // sort of arbitrary but it seems like a good amount?
    }

    @Override
    public void onLostControl() {
        // TODO reset ourselves?
        logDirect("storeExcessInventory cannot be used with allowInventory false");
    }

    @Override
    public String displayName0() {
        // TODO figure out if we're storing or obtaining
        return "Inventory Store" + filter.toString();
    }

    /**
     * TODO: move this to obtain
     * Scans for and returns the block pos off the scanned items
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
    }

    @Override
    public void storeBlocksByName(int quantity, String... blocks) {
        // TODO Auto-generated method stub
        this.desiredQuantity = quantity;
        this.filter = new BlockOptionalMetaLookup(blocks);

    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        // TODO Auto-generated method stub
        logDirect("storeExcessInventory- tick");
        return new PathingCommand(null, PathingCommandType.DEFER); // cede to other process
    }
}
