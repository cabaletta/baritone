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
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.stream.Collectors;

import static baritone.api.pathing.movement.ActionCosts.COST_INF;

// TODO consider splitting this into a store and a obtain process?
// How do you prevent them from competing?

/**
 * Mine blocks of a certain type
 *
 * @author matthewfcarlson
 */
public final class InventoryStoreProcess extends BaritoneProcessHelper implements IInventoryStoreProcess {

    private BlockOptionalMetaLookup filter;
    private int desiredQuantity;
    private int tickCount;

    private enum EInventoryProcessState {
        OBTAIN,
        IDLE,
        STORE
    }
    private EInventoryProcessState state = EInventoryProcessState.IDLE;

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
        
        // TODO check if we want to be active 
        // We only want to check if the inventory if full
        return state != EInventoryProcessState.IDLE;
    }

    @Override
    public double priority() {
        return 2; // sort of arbitrary but it seems like a good amount?
    }

    @Override
    public void onLostControl() {
        // TODO reset ourselves?
    }

    @Override
    public String displayName0() {
        // TODO figure out if we're storing or obtaining
        return "Inventory " + filter;
    }

    /**
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
    public void obtainBlocks(int quantity, BlockOptionalMetaLookup filter) {
        // TODO Auto-generated method stub
        this.desiredQuantity = quantity;

    }

    @Override
    public void obtainBlocksByName(int quantity, String... blocks) {
        // TODO Auto-generated method stub
        this.desiredQuantity = quantity;

    }

    @Override
    public void storeBlocks(int quantity, BlockOptionalMetaLookup filter) {
        // TODO Auto-generated method stub
        this.desiredQuantity = quantity;

    }

    @Override
    public void storeBlocksByName(int quantity, String... blocks) {
        // TODO Auto-generated method stub
        this.desiredQuantity = quantity;

    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        // TODO Auto-generated method stub
        return new PathingCommand(null, PathingCommandType.DEFER); // cede to other process
    }
}
