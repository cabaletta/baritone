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
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalComposite;
import baritone.api.process.ILootProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.pathing.movement.CalculationContext;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static baritone.api.pathing.movement.ActionCosts.COST_INF;

public final class LootProcess  extends BaritoneProcessHelper implements ILootProcess {

    private static final int MAX_CONTAINER_POSITIONS = 64;

    private List<BlockPos> knownContainerPositions;
    private List<BlockPos> blacklist; // inaccessible
    private int desiredContainers;
    private int containersLeft;
    private List<Item> itemsToCollect;

    public LootProcess(Baritone baritone) { super(baritone); }

    @Override
    public boolean isActive() { return containersLeft != 0; }

    @Override
    public void loot(int amount, List<Item> items) {
        itemsToCollect = items;
        desiredContainers = amount;
        containersLeft = desiredContainers;
        knownContainerPositions = new ArrayList<>();
        blacklist = new ArrayList<>();
        itemsToCollect = items;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (containersLeft != 0) {
            logDirect("Looting of " + desiredContainers + " containers completed.");
            cancel();
        }
        if (calcFailed) {
            if (!knownContainerPositions.isEmpty() && Baritone.settings().blacklistClosestOnFailure.value) {
                logDirect("Unable to find any path to a valid container, blacklisting presumably unreachable closest instance...");
                knownContainerPositions.stream().min(Comparator.comparingDouble(ctx.player()::getDistanceSq)).ifPresent(blacklist::add);
                knownContainerPositions.removeIf(blacklist::contains);
            } else {
                logDirect("Unable to find any path to a valid container, cancelling loot");
                cancel();
                return null;
            }
        }
        if (Baritone.settings().stealContainer.value && !Baritone.settings().allowBreak.value) {
            logDirect("Cannot steal the containers when allowBreak is false!");
            cancel();
            return null;
        }
        return null;
    }

    private PathingCommand updateGoal() {
        List<BlockPos> locs = knownContainerPositions;
        if (!locs.isEmpty()) {
            CalculationContext context = new CalculationContext(baritone);
            List<BlockPos> locs2 = prune(context, new ArrayList<>(locs), Baritone.settings().lootContainers.value, MAX_CONTAINER_POSITIONS, blacklist, droppedItemsScan());
            // can't reassign locs, gotta make a new var locs2, because we use it in a lambda right here, and variables you use in a lambda must be effectively final
            Goal goal = new GoalComposite(locs2.stream().map(GoalBlock::new).toArray(Goal[]::new));
            knownContainerPositions = locs2;
            return new PathingCommand(goal, PathingCommandType.REVALIDATE_GOAL_AND_PATH);
        }
        return null;
    }

    @Override
    public void onLostControl() {
        knownContainerPositions = null;
        desiredContainers = 0;
        containersLeft = 0;
        itemsToCollect = null;
    }

    private final static List<Block> shulkers = new ArrayList<>();

    public static boolean plausibleToLoot(CalculationContext ctx, BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        IBlockState state = ctx.get(pos);
        Block block = state.getBlock();
        if (shulkers.contains(block)) {
            EnumFacing direction = state.getValue(PropertyDirection.create(("facing")));
            IBlockState topState;
            Block top;
            switch (direction) {
                case UP:
                default:
                    topState = ctx.get(x, y + 1, z);
                    break;
                case DOWN:
                    topState = ctx.get(x, y - 1, z);
                    break;
                case NORTH:
                    topState = ctx.get(x + 1, y, z);
                    break;
                case SOUTH:
                    topState = ctx.get(x - 1, y, z);
                    break;
                case EAST:
                    topState = ctx.get(x, y, z + 1);
                    break;
                case WEST:
                    topState = ctx.get(x, y, z - 1);
            }
            top = topState.getBlock();
            return (topState.isTranslucent() && !topState.isOpaqueCube()) || top == Blocks.AIR;
            // correct me if this isn't how shulkers work
        } else if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.ENDER_CHEST) { // idk why you would want to loot a echest but whatever
            return ctx.getBlock(x, y + 1, z) == Blocks.AIR;
        } else {
            return block instanceof BlockContainer;
        }
    }

    private static List<BlockPos> prune(CalculationContext ctx, List<BlockPos> locs2, List<Block> containers, int max, List<BlockPos> blacklist, List<BlockPos> dropped) {
        dropped.removeIf(drop -> {
            for (BlockPos pos : locs2) {
                if (pos.distanceSq(drop) <= 9 && containers.contains(ctx.get(pos.getX(), pos.getY(), pos.getZ())) && plausibleToLoot(ctx, pos)) { // TODO maybe drop also has to be supported? no lava below?
                    return true;
                }
            }
            return false;
        });
        List<BlockPos> locs = locs2
                .stream()
                .distinct()

                // remove any that are within loaded chunks that aren't actually what we want
                .filter(pos -> !ctx.bsi.worldContainsLoadedChunk(pos.getX(), pos.getZ()) || containers.contains(ctx.get(pos.getX(), pos.getY(), pos.getZ())) || dropped.contains(pos))

                // remove any that are implausible to mine (encased in bedrock, or touching lava)
                .filter(pos -> MineProcess.plausibleToBreak(ctx, pos))

                .filter(pos -> !blacklist.contains(pos))

                .sorted(Comparator.comparingDouble(ctx.getBaritone().getPlayerContext().player()::getDistanceSq))
                .collect(Collectors.toList());

        if (locs.size() > max) {
            return locs.subList(0, max);
        }
        return locs;
    }

    public List<BlockPos> droppedItemsScan() {
        if (!Baritone.settings().lootScanDroppedItems.value) {
            return Collections.emptyList();
        }
        List<BlockPos> ret = new ArrayList<>();
        for (Entity entity : ctx.world().loadedEntityList) {
            if (entity instanceof EntityItem) {
                EntityItem ei = (EntityItem) entity;
                if (itemsToCollect.contains(ei.getItem())) {
                    ret.add(new BlockPos(entity));
                }
            }
        }
        return ret;
    }

    @Override
    public String displayName0() {
        return "Loot Containers";
    }

}
