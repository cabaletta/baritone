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
import baritone.api.behavior.IMineBehavior;
import baritone.api.event.events.TickEvent;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalComposite;
import baritone.api.pathing.goals.GoalTwoBlocks;
import baritone.cache.CachedChunk;
import baritone.cache.ChunkPacker;
import baritone.cache.WorldProvider;
import baritone.cache.WorldScanner;
import baritone.utils.BlockStateInterface;
import baritone.utils.Helper;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.EmptyChunk;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Mine blocks of a certain type
 *
 * @author leijurv
 */
public final class MineBehavior extends Behavior implements IMineBehavior, Helper {

    public static final MineBehavior INSTANCE = new MineBehavior();

    private List<Block> mining;
    private List<BlockPos> locationsCache;
    private int quantity;

    private MineBehavior() {}

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT) {
            cancel();
            return;
        }
        if (mining == null) {
            return;
        }
        if (quantity > 0) {
            Item item = mining.get(0).getItemDropped(mining.get(0).getDefaultState(), new Random(), 0);
            int curr = player().inventory.mainInventory.stream().filter(stack -> item.equals(stack.getItem())).mapToInt(ItemStack::getCount).sum();
            System.out.println("Currently have " + curr + " " + item);
            if (curr >= quantity) {
                logDirect("Have " + curr + " " + item.getItemStackDisplayName(new ItemStack(item, 1)));
                cancel();
                return;
            }
        }
        int mineGoalUpdateInterval = Baritone.settings().mineGoalUpdateInterval.get();
        if (mineGoalUpdateInterval != 0) {
            if (event.getCount() % mineGoalUpdateInterval == 0) {
                Baritone.INSTANCE.getExecutor().execute(this::rescan);
            }
        }
        updateGoal();
        PathingBehavior.INSTANCE.revalidateGoal();
    }

    private void updateGoal() {
        if (mining == null) {
            return;
        }
        List<BlockPos> locs = locationsCache;
        if (!locs.isEmpty()) {
            locs = prune(new ArrayList<>(locs), mining, 64);
            PathingBehavior.INSTANCE.setGoal(coalesce(locs));
            PathingBehavior.INSTANCE.path();
            locationsCache = locs;
        }
    }

    private void rescan() {
        if (mining == null) {
            return;
        }
        List<BlockPos> locs = scanFor(mining, 64);
        if (locs.isEmpty()) {
            logDebug("No locations for " + mining + " known, cancelling");
            mine(0, (String[]) null);
            return;
        }
        locationsCache = locs;
    }

    public Goal coalesce(BlockPos loc, List<BlockPos> locs) {
        if (!Baritone.settings().forceInternalMining.get()) {
            return new GoalTwoBlocks(loc);
        }

        boolean upwardGoal = locs.contains(loc.up()) || (Baritone.settings().internalMiningAirException.get() && BlockStateInterface.getBlock(loc.up()) == Blocks.AIR);
        boolean downwardGoal = locs.contains(loc.down()) || (Baritone.settings().internalMiningAirException.get() && BlockStateInterface.getBlock(loc.up()) == Blocks.AIR);
        if (upwardGoal) {
            if (downwardGoal) {
                return new GoalTwoBlocks(loc);
            } else {
                return new GoalBlock(loc);
            }
        } else {
            if (downwardGoal) {
                return new GoalBlock(loc.down());
            } else {
                return new GoalTwoBlocks(loc);
            }
        }
    }

    public GoalComposite coalesce(List<BlockPos> locs) {
        return new GoalComposite(locs.stream().map(loc -> coalesce(loc, locs)).toArray(Goal[]::new));
    }

    public List<BlockPos> scanFor(List<Block> mining, int max) {
        List<BlockPos> locs = new ArrayList<>();
        List<Block> uninteresting = new ArrayList<>();
        //long b = System.currentTimeMillis();
        for (Block m : mining) {
            if (CachedChunk.BLOCKS_TO_KEEP_TRACK_OF.contains(m)) {
                locs.addAll(WorldProvider.INSTANCE.getCurrentWorld().getCachedWorld().getLocationsOf(ChunkPacker.blockToString(m), 1, 1));
            } else {
                uninteresting.add(m);
            }
        }
        //System.out.println("Scan of cached chunks took " + (System.currentTimeMillis() - b) + "ms");
        if (locs.isEmpty()) {
            uninteresting = mining;
        }
        if (!uninteresting.isEmpty()) {
            //long before = System.currentTimeMillis();
            locs.addAll(WorldScanner.INSTANCE.scanChunkRadius(uninteresting, max, 10, 26));
            //System.out.println("Scan of loaded chunks took " + (System.currentTimeMillis() - before) + "ms");
        }
        return prune(locs, mining, max);
    }

    public List<BlockPos> prune(List<BlockPos> locs, List<Block> mining, int max) {
        BlockPos playerFeet = MineBehavior.INSTANCE.playerFeet();
        locs.sort(Comparator.comparingDouble(playerFeet::distanceSq));

        // remove any that are within loaded chunks that aren't actually what we want
        locs.removeAll(locs.stream()
                .filter(pos -> !(MineBehavior.INSTANCE.world().getChunk(pos) instanceof EmptyChunk))
                .filter(pos -> !mining.contains(BlockStateInterface.get(pos).getBlock()))
                .collect(Collectors.toList()));
        if (locs.size() > max) {
            return locs.subList(0, max);
        }
        return locs;
    }

    @Override
    public void mine(int quantity, String... blocks) {
        this.mining = blocks == null || blocks.length == 0 ? null : Arrays.stream(blocks).map(ChunkPacker::stringToBlock).collect(Collectors.toList());
        this.quantity = quantity;
        this.locationsCache = new ArrayList<>();
        rescan();
        updateGoal();
    }

    @Override
    public void mine(int quantity, Block... blocks) {
        this.mining = blocks == null || blocks.length == 0 ? null : Arrays.asList(blocks);
        this.quantity = quantity;
        this.locationsCache = new ArrayList<>();
        rescan();
        updateGoal();
    }

    @Override
    public void cancel() {
        mine(0, (String[]) null);
        PathingBehavior.INSTANCE.cancel();
    }
}
