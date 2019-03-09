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
import baritone.cache.WorldScanner;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.MovementHelper;
import baritone.utils.BaritoneProcessHelper;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
            Item item = mining.get(0).getItemDropped(mining.get(0).getDefaultState(), ctx.world(), null, 0).asItem();
            int curr = ctx.player().inventory.mainInventory.stream().filter(stack -> item.equals(stack.getItem())).mapToInt(ItemStack::getCount).sum();
            System.out.println("Currently have " + curr + " " + item);
            if (curr >= desiredQuantity) {
                logDirect("Have " + curr + " " + item.getDisplayName(new ItemStack(item, 1)));
                cancel();
                return null;
            }
        }
        if (calcFailed) {
            logDirect("Unable to find any path to " + mining + ", canceling Mine");
            cancel();
            return null;
        }
        int mineGoalUpdateInterval = Baritone.settings().mineGoalUpdateInterval.value;
        if (mineGoalUpdateInterval != 0 && tickCount++ % mineGoalUpdateInterval == 0) { // big brain
            List<BlockPos> curr = new ArrayList<>(knownOreLocations);
            CalculationContext context = new CalculationContext(baritone, true);
            Baritone.getExecutor().execute(() -> rescan(curr, context));
        }
        if (Baritone.settings().legitMine.value) {
            addNearby();
        }
        PathingCommand command = updateGoal();
        if (command == null) {
            // none in range
            // maybe say something in chat? (ahem impact)
            cancel();
            return null;
        }
        return command;
    }

    @Override
    public void onLostControl() {
        mine(0, (Block[]) null);
    }

    @Override
    public String displayName() {
        return "Mine " + mining;
    }

    private PathingCommand updateGoal() {
        boolean legit = Baritone.settings().legitMine.value;
        List<BlockPos> locs = knownOreLocations;
        if (!locs.isEmpty()) {
            List<BlockPos> locs2 = prune(new CalculationContext(baritone), new ArrayList<>(locs), mining, ORE_LOCATIONS_COUNT);
            // can't reassign locs, gotta make a new var locs2, because we use it in a lambda right here, and variables you use in a lambda must be effectively final
            Goal goal = new GoalComposite(locs2.stream().map(loc -> coalesce(ctx, loc, locs2)).toArray(Goal[]::new));
            knownOreLocations = locs2;
            return new PathingCommand(goal, legit ? PathingCommandType.FORCE_REVALIDATE_GOAL_AND_PATH : PathingCommandType.REVALIDATE_GOAL_AND_PATH);
        }
        // we don't know any ore locations at the moment
        if (!legit) {
            return null;
        }
        // only in non-Xray mode (aka legit mode) do we do this
        int y = Baritone.settings().legitMineYLevel.value;
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
            branchPointRunaway = new GoalRunAway(1, y, branchPoint) {
                @Override
                public boolean isInGoal(int x, int y, int z) {
                    return false;
                }
            };
        }
        return new PathingCommand(branchPointRunaway, PathingCommandType.REVALIDATE_GOAL_AND_PATH);
    }

    private void rescan(List<BlockPos> already, CalculationContext context) {
        if (mining == null) {
            return;
        }
        if (Baritone.settings().legitMine.value) {
            return;
        }
        List<BlockPos> locs = searchWorld(context, mining, ORE_LOCATIONS_COUNT, already);
        locs.addAll(droppedItemsScan(mining, ctx.world()));
        if (locs.isEmpty()) {
            logDebug("No locations for " + mining + " known, cancelling");
            cancel();
            return;
        }
        knownOreLocations = locs;
    }

    private static Goal coalesce(IPlayerContext ctx, BlockPos loc, List<BlockPos> locs) {
        if (!Baritone.settings().forceInternalMining.value) {
            return new GoalTwoBlocks(loc);
        }

        // Here, BlockStateInterface is used because the position may be in a cached chunk (the targeted block is one that is kept track of)
        boolean upwardGoal = locs.contains(loc.up()) || (Baritone.settings().internalMiningAirException.value && BlockStateInterface.getBlock(ctx, loc.up()) == Blocks.AIR);
        boolean downwardGoal = locs.contains(loc.down()) || (Baritone.settings().internalMiningAirException.value && BlockStateInterface.getBlock(ctx, loc.down()) == Blocks.AIR);
        return upwardGoal == downwardGoal ? new GoalTwoBlocks(loc) : upwardGoal ? new GoalBlock(loc) : new GoalBlock(loc.down());
    }

    public static List<BlockPos> droppedItemsScan(List<Block> mining, World world) {
        if (!Baritone.settings().mineScanDroppedItems.value) {
            return new ArrayList<>();
        }
        Set<Item> searchingFor = new HashSet<>();
        for (Block block : mining) {
            Item drop = block.getItemDropped(block.getDefaultState(), world, null, 0).asItem();
            Item ore = block.asItem();
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

    public static List<BlockPos> searchWorld(CalculationContext ctx, List<Block> mining, int max, List<BlockPos> alreadyKnown) {
        List<BlockPos> locs = new ArrayList<>();
        List<Block> uninteresting = new ArrayList<>();
        //long b = System.currentTimeMillis();
        for (Block m : mining) {
            if (CachedChunk.BLOCKS_TO_KEEP_TRACK_OF.contains(m)) {
                // maxRegionDistanceSq 2 means adjacent directly or adjacent diagonally; nothing further than that
                locs.addAll(ctx.worldData.getCachedWorld().getLocationsOf(ChunkPacker.blockToString(m), Baritone.settings().maxCachedWorldScanCount.value, ctx.getBaritone().getPlayerContext().playerFeet().getX(), ctx.getBaritone().getPlayerContext().playerFeet().getZ(), 2));
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
            locs.addAll(WorldScanner.INSTANCE.scanChunkRadius(ctx.getBaritone().getPlayerContext(), uninteresting, max, 10, 32)); // maxSearchRadius is NOT sq
            //System.out.println("Scan of loaded chunks took " + (System.currentTimeMillis() - before) + "ms");
        }
        locs.addAll(alreadyKnown);
        return prune(ctx, locs, mining, max);
    }

    private void addNearby() {
        knownOreLocations.addAll(droppedItemsScan(mining, ctx.world()));
        BlockPos playerFeet = ctx.playerFeet();
        BlockStateInterface bsi = new BlockStateInterface(ctx);
        int searchDist = 10;
        double fakedBlockReachDistance = 20; // at least 10 * sqrt(3) with some extra space to account for positioning within the block
        for (int x = playerFeet.getX() - searchDist; x <= playerFeet.getX() + searchDist; x++) {
            for (int y = playerFeet.getY() - searchDist; y <= playerFeet.getY() + searchDist; y++) {
                for (int z = playerFeet.getZ() - searchDist; z <= playerFeet.getZ() + searchDist; z++) {
                    // crucial to only add blocks we can see because otherwise this
                    // is an x-ray and it'll get caught
                    if (mining.contains(bsi.get0(x, y, z).getBlock())) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if ((Baritone.settings().legitMineIncludeDiagonals.value && knownOreLocations.stream().anyMatch(ore -> ore.distanceSq(pos) <= 2 /* sq means this is pytha dist <= sqrt(2) */)) || RotationUtils.reachable(ctx.player(), pos, fakedBlockReachDistance).isPresent()) {
                            knownOreLocations.add(pos);
                        }
                    }
                }
            }
        }
        knownOreLocations = prune(new CalculationContext(baritone), knownOreLocations, mining, ORE_LOCATIONS_COUNT);
    }

    public static List<BlockPos> prune(CalculationContext ctx, List<BlockPos> locs2, List<Block> mining, int max) {
        List<BlockPos> dropped = droppedItemsScan(mining, ctx.world);
        dropped.removeIf(drop -> {
            for (BlockPos pos : locs2) {
                if (pos.distanceSq(drop) <= 9 && mining.contains(ctx.getBlock(pos.getX(), pos.getY(), pos.getZ())) && MineProcess.plausibleToBreak(ctx.bsi, pos)) { // TODO maybe drop also has to be supported? no lava below?
                    return true;
                }
            }
            return false;
        });
        List<BlockPos> locs = locs2
                .stream()
                .distinct()

                // remove any that are within loaded chunks that aren't actually what we want
                .filter(pos -> !ctx.bsi.worldContainsLoadedChunk(pos.getX(), pos.getZ()) || mining.contains(ctx.getBlock(pos.getX(), pos.getY(), pos.getZ())) || dropped.contains(pos))

                // remove any that are implausible to mine (encased in bedrock, or touching lava)
                .filter(pos -> MineProcess.plausibleToBreak(ctx.bsi, pos))

                .sorted(Comparator.comparingDouble(ctx.getBaritone().getPlayerContext().playerFeet()::distanceSq))
                .collect(Collectors.toList());

        if (locs.size() > max) {
            return locs.subList(0, max);
        }
        return locs;
    }

    public static boolean plausibleToBreak(BlockStateInterface bsi, BlockPos pos) {
        if (MovementHelper.avoidBreaking(bsi, pos.getX(), pos.getY(), pos.getZ(), bsi.get0(pos))) {
            return false;
        }

        // bedrock above and below makes it implausible, otherwise we're good
        return !(bsi.get0(pos.up()).getBlock() == Blocks.BEDROCK && bsi.get0(pos.down()).getBlock() == Blocks.BEDROCK);
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
        if (mining != null) {
            rescan(new ArrayList<>(), new CalculationContext(baritone));
        }
    }
}
