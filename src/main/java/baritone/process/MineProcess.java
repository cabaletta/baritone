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
import baritone.api.process.IMineProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.RotationUtils;
import baritone.cache.CachedChunk;
import baritone.cache.ChunkPacker;
import baritone.cache.WorldProvider;
import baritone.cache.WorldScanner;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.MovementHelper;
import baritone.utils.BaritoneProcessHelper;
import baritone.utils.BlockStateInterface;
import baritone.utils.Helper;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.EmptyChunk;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Mine blocks of a certain type
 *
 * @author leijurv
 */
public final class MineProcess extends BaritoneProcessHelper implements IMineProcess {

    private static final int ORE_LOCATIONS_COUNT = 64;

    private List<Block> mining;
    private List<BlockPos> knownOreLocations;
    private BlockPos branchPoint;
    private GoalRunAway branchPointRunaway;
    private int desiredQuantity;
    private int tickCount;

    public MineProcess(Baritone baritone) {
        super(baritone, 0);
    }

    @Override
    public boolean isActive() {
        return mining != null;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (desiredQuantity > 0) {
            Item item = mining.get(0).getItemDropped(mining.get(0).getDefaultState(), new Random(), 0);
            int curr = ctx.player().inventory.mainInventory.stream().filter(stack -> item.equals(stack.getItem())).mapToInt(ItemStack::getCount).sum();
            System.out.println("Currently have " + curr + " " + item);
            if (curr >= desiredQuantity) {
                logDirect("Have " + curr + " " + item.getItemStackDisplayName(new ItemStack(item, 1)));
                cancel();
                return null;
            }
        }
        if (calcFailed) {
            logDirect("Unable to find any path to " + mining + ", canceling Mine");
            cancel();
            return null;
        }
        int mineGoalUpdateInterval = Baritone.settings().mineGoalUpdateInterval.get();
        if (mineGoalUpdateInterval != 0 && tickCount++ % mineGoalUpdateInterval == 0) { // big brain
            List<BlockPos> curr = new ArrayList<>(knownOreLocations);
            baritone.getExecutor().execute(() -> rescan(curr));
        }
        if (Baritone.settings().legitMine.get()) {
            addNearby();
        }
        Goal goal = updateGoal();
        if (goal == null) {
            // none in range
            // maybe say something in chat? (ahem impact)
            cancel();
            return null;
        }
        return new PathingCommand(goal, PathingCommandType.FORCE_REVALIDATE_GOAL_AND_PATH);
    }

    @Override
    public void onLostControl() {
        mine(0, (Block[]) null);
    }

    @Override
    public String displayName() {
        return "Mine " + mining;
    }

    private Goal updateGoal() {
        List<BlockPos> locs = knownOreLocations;
        if (!locs.isEmpty()) {
            List<BlockPos> locs2 = prune(new ArrayList<>(locs), mining, ORE_LOCATIONS_COUNT);
            // can't reassign locs, gotta make a new var locs2, because we use it in a lambda right here, and variables you use in a lambda must be effectively final
            Goal goal = new GoalComposite(locs2.stream().map(loc -> coalesce(loc, locs2)).toArray(Goal[]::new));
            knownOreLocations = locs2;
            return goal;
        }
        // we don't know any ore locations at the moment
        if (!Baritone.settings().legitMine.get()) {
            return null;
        }
        // only in non-Xray mode (aka legit mode) do we do this
        int y = Baritone.settings().legitMineYLevel.get();
        if (branchPoint == null) {
            /*if (!baritone.getPathingBehavior().isPathing() && playerFeet().y == y) {
                // cool, path is over and we are at desired y
                branchPoint = playerFeet();
                branchPointRunaway = null;
            } else {
                return new GoalYLevel(y);
            }*/
            branchPoint = ctx.playerFeet();
        }
        // TODO shaft mode, mine 1x1 shafts to either side
        // TODO also, see if the GoalRunAway with maintain Y at 11 works even from the surface
        if (branchPointRunaway == null) {
            branchPointRunaway = new GoalRunAway(1, Optional.of(y), branchPoint) {
                @Override
                public boolean isInGoal(int x, int y, int z) {
                    return false;
                }
            };
        }
        return branchPointRunaway;
    }

    private void rescan(List<BlockPos> already) {
        if (mining == null) {
            return;
        }
        if (Baritone.settings().legitMine.get()) {
            return;
        }
        List<BlockPos> locs = searchWorld(mining, ORE_LOCATIONS_COUNT, baritone.getWorldProvider(), already);
        locs.addAll(droppedItemsScan(mining, ctx.world()));
        if (locs.isEmpty()) {
            logDebug("No locations for " + mining + " known, cancelling");
            cancel();
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

    public static List<BlockPos> droppedItemsScan(List<Block> mining, World world) {
        if (!Baritone.settings().mineScanDroppedItems.get()) {
            return new ArrayList<>();
        }
        Set<Item> searchingFor = new HashSet<>();
        for (Block block : mining) {
            Item drop = block.getItemDropped(block.getDefaultState(), new Random(), 0);
            Item ore = Item.getItemFromBlock(block);
            searchingFor.add(drop);
            searchingFor.add(ore);
        }
        List<BlockPos> ret = new ArrayList<>();
        for (Entity entity : world.loadedEntityList) {
            if (entity instanceof EntityItem) {
                EntityItem ei = (EntityItem) entity;
                if (searchingFor.contains(ei.getItem().getItem())) {
                    ret.add(new BlockPos(entity));
                }
            }
        }
        return ret;
    }

    /*public static List<BlockPos> searchWorld(List<Block> mining, int max, World world) {

    }*/
    public List<BlockPos> searchWorld(List<Block> mining, int max, WorldProvider provider, List<BlockPos> alreadyKnown) {
        List<BlockPos> locs = new ArrayList<>();
        List<Block> uninteresting = new ArrayList<>();
        //long b = System.currentTimeMillis();
        for (Block m : mining) {
            if (CachedChunk.BLOCKS_TO_KEEP_TRACK_OF.contains(m)) {
                locs.addAll(provider.getCurrentWorld().getCachedWorld().getLocationsOf(ChunkPacker.blockToString(m), 1, ctx.playerFeet().getX(), ctx.playerFeet().getZ(), 1));
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
            locs.addAll(WorldScanner.INSTANCE.scanChunkRadius(ctx, uninteresting, max, 10, 26));
            //System.out.println("Scan of loaded chunks took " + (System.currentTimeMillis() - before) + "ms");
        }
        locs.addAll(alreadyKnown);
        return prune(locs, mining, max);
    }

    public void addNearby() {
        knownOreLocations.addAll(droppedItemsScan(mining, ctx.world()));
        BlockPos playerFeet = ctx.playerFeet();
        int searchDist = 4; // why four? idk
        for (int x = playerFeet.getX() - searchDist; x <= playerFeet.getX() + searchDist; x++) {
            for (int y = playerFeet.getY() - searchDist; y <= playerFeet.getY() + searchDist; y++) {
                for (int z = playerFeet.getZ() - searchDist; z <= playerFeet.getZ() + searchDist; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    // crucial to only add blocks we can see because otherwise this is an x-ray and it'll get caught
                    if (mining.contains(BlockStateInterface.getBlock(pos)) && RotationUtils.reachable(ctx.player(), pos, ctx.playerController().getBlockReachDistance()).isPresent()) {
                        knownOreLocations.add(pos);
                    }
                }
            }
        }
        knownOreLocations = prune(knownOreLocations, mining, ORE_LOCATIONS_COUNT);
    }

    public List<BlockPos> prune(List<BlockPos> locs2, List<Block> mining, int max) {
        List<BlockPos> dropped = droppedItemsScan(mining, ctx.world());
        List<BlockPos> locs = locs2
                .stream()
                .distinct()

                // remove any that are within loaded chunks that aren't actually what we want
                .filter(pos -> ctx.world().getChunk(pos) instanceof EmptyChunk || mining.contains(BlockStateInterface.get(pos).getBlock()) || dropped.contains(pos))

                // remove any that are implausible to mine (encased in bedrock, or touching lava)
                .filter(this::plausibleToBreak)

                .sorted(Comparator.comparingDouble(ctx.playerFeet()::distanceSq))
                .collect(Collectors.toList());

        if (locs.size() > max) {
            return locs.subList(0, max);
        }
        return locs;
    }

    public boolean plausibleToBreak(BlockPos pos) {
        if (MovementHelper.avoidBreaking(new CalculationContext(baritone), pos.getX(), pos.getY(), pos.getZ(), BlockStateInterface.get(pos))) {
            return false;
        }
        // bedrock above and below makes it implausible, otherwise we're good
        return !(BlockStateInterface.getBlock(pos.up()) == Blocks.BEDROCK && BlockStateInterface.getBlock(pos.down()) == Blocks.BEDROCK);
    }

    @Override
    public void mineByName(int quantity, String... blocks) {
        mine(quantity, blocks == null || blocks.length == 0 ? null : Arrays.stream(blocks).map(ChunkPacker::stringToBlock).toArray(Block[]::new));
    }

    @Override
    public void mine(int quantity, Block... blocks) {
        this.mining = blocks == null || blocks.length == 0 ? null : Arrays.asList(blocks);
        this.desiredQuantity = quantity;
        this.knownOreLocations = new ArrayList<>();
        this.branchPoint = null;
        this.branchPointRunaway = null;
        rescan(new ArrayList<>());
    }
}
