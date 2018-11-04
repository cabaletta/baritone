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
import baritone.api.pathing.goals.*;
import baritone.api.utils.RotationUtils;
import baritone.cache.CachedChunk;
import baritone.cache.ChunkPacker;
import baritone.cache.WorldProvider;
import baritone.cache.WorldScanner;
import baritone.pathing.movement.MovementHelper;
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

    private List<Block> mining;
    private List<BlockPos> knownOreLocations;
    private BlockPos branchPoint;
    private int desiredQuantity;

    public MineBehavior(Baritone baritone) {
        super(baritone);
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT) {
            cancel();
            return;
        }
        if (mining == null) {
            return;
        }
        if (desiredQuantity > 0) {
            Item item = mining.get(0).getItemDropped(mining.get(0).getDefaultState(), new Random(), 0);
            int curr = player().inventory.mainInventory.stream().filter(stack -> item.equals(stack.getItem())).mapToInt(ItemStack::getCount).sum();
            System.out.println("Currently have " + curr + " " + item);
            if (curr >= desiredQuantity) {
                logDirect("Have " + curr + " " + item.getItemStackDisplayName(new ItemStack(item, 1)));
                cancel();
                return;
            }
        }
        int mineGoalUpdateInterval = Baritone.settings().mineGoalUpdateInterval.get();
        if (mineGoalUpdateInterval != 0 && event.getCount() % mineGoalUpdateInterval == 0) {
            Baritone.INSTANCE.getExecutor().execute(this::rescan);
        }
        if (Baritone.settings().legitMine.get()) {
            addNearby();
        }
        updateGoal();
        baritone.getPathingBehavior().revalidateGoal();
    }

    private void updateGoal() {
        if (mining == null) {
            return;
        }
        List<BlockPos> locs = knownOreLocations;
        if (!locs.isEmpty()) {
            List<BlockPos> locs2 = prune(new ArrayList<>(locs), mining, 64);
            // can't reassign locs, gotta make a new var locs2, because we use it in a lambda right here, and variables you use in a lambda must be effectively final
            baritone.getPathingBehavior().setGoalAndPath(new GoalComposite(locs2.stream().map(loc -> coalesce(loc, locs2)).toArray(Goal[]::new)));
            knownOreLocations = locs2;
            return;
        }
        // we don't know any ore locations at the moment
        if (!Baritone.settings().legitMine.get()) {
            return;
        }
        // only in non-Xray mode (aka legit mode) do we do this
        if (branchPoint == null) {
            int y = Baritone.settings().legitMineYLevel.get();
            if (!baritone.getPathingBehavior().isPathing() && playerFeet().y == y) {
                // cool, path is over and we are at desired y
                branchPoint = playerFeet();
            } else {
                baritone.getPathingBehavior().setGoalAndPath(new GoalYLevel(y));
                return;
            }
        }

        if (playerFeet().equals(branchPoint)) {
            // TODO mine 1x1 shafts to either side
            branchPoint = branchPoint.north(10);
        }
        baritone.getPathingBehavior().setGoalAndPath(new GoalBlock(branchPoint));
    }

    private void rescan() {
        if (mining == null) {
            return;
        }
        if (Baritone.settings().legitMine.get()) {
            return;
        }
        List<BlockPos> locs = searchWorld(mining, 64);
        if (locs.isEmpty()) {
            logDebug("No locations for " + mining + " known, cancelling");
            mine(0, (String[]) null);
            return;
        }
        knownOreLocations = locs;
    }

    private static Goal coalesce(BlockPos loc, List<BlockPos> locs) {
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

    public List<BlockPos> searchWorld(List<Block> mining, int max) {
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

    public void addNearby() {
        BlockPos playerFeet = playerFeet();
        int searchDist = 4;//why four? idk
        for (int x = playerFeet.getX() - searchDist; x <= playerFeet.getX() + searchDist; x++) {
            for (int y = playerFeet.getY() - searchDist; y <= playerFeet.getY() + searchDist; y++) {
                for (int z = playerFeet.getZ() - searchDist; z <= playerFeet.getZ() + searchDist; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (mining.contains(BlockStateInterface.getBlock(pos)) && RotationUtils.reachable(player(), pos).isPresent()) {//crucial to only add blocks we can see because otherwise this is an x-ray and it'll get caught
                        knownOreLocations.add(pos);
                    }
                }
            }
        }
        knownOreLocations = prune(knownOreLocations, mining, 64);
    }

    public List<BlockPos> prune(List<BlockPos> locs2, List<Block> mining, int max) {
        List<BlockPos> locs = locs2
                .stream()

                // remove any that are within loaded chunks that aren't actually what we want
                .filter(pos -> world().getChunk(pos) instanceof EmptyChunk || mining.contains(BlockStateInterface.get(pos).getBlock()))

                // remove any that are implausible to mine (encased in bedrock, or touching lava)
                .filter(MineBehavior::plausibleToBreak)

                .sorted(Comparator.comparingDouble(Helper.HELPER.playerFeet()::distanceSq))
                .collect(Collectors.toList());

        if (locs.size() > max) {
            return locs.subList(0, max);
        }
        return locs;
    }

    public static boolean plausibleToBreak(BlockPos pos) {
        if (MovementHelper.avoidBreaking(pos.getX(), pos.getY(), pos.getZ(), BlockStateInterface.get(pos))) {
            return false;
        }
        // bedrock above and below makes it implausible, otherwise we're good
        return !(BlockStateInterface.getBlock(pos.up()) == Blocks.BEDROCK && BlockStateInterface.getBlock(pos.down()) == Blocks.BEDROCK);
    }

    @Override
    public void mine(int quantity, String... blocks) {
        this.mining = blocks == null || blocks.length == 0 ? null : Arrays.stream(blocks).map(ChunkPacker::stringToBlock).collect(Collectors.toList());
        this.desiredQuantity = quantity;
        this.knownOreLocations = new ArrayList<>();
        this.branchPoint = null;
        rescan();
        updateGoal();
    }

    @Override
    public void mine(int quantity, Block... blocks) {
        this.mining = blocks == null || blocks.length == 0 ? null : Arrays.asList(blocks);
        this.desiredQuantity = quantity;
        this.knownOreLocations = new ArrayList<>();
        this.branchPoint = null;
        rescan();
        updateGoal();
    }

    @Override
    public void cancel() {
        mine(0, (String[]) null);
        baritone.getPathingBehavior().cancel();
    }
}
