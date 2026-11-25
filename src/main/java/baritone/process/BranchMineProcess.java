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
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalTwoBlocks;
import baritone.api.process.IBranchMineProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.selection.ISelection;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockOptionalMeta;
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.MovementHelper;
import baritone.utils.BaritoneProcessHelper;
import baritone.utils.BlockStateInterface;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class BranchMineProcess extends BaritoneProcessHelper implements IBranchMineProcess {

    private static final List<Block> DEFAULT_ORES = Arrays.asList(
            Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.COPPER_ORE, Blocks.GOLD_ORE,
            Blocks.REDSTONE_ORE, Blocks.LAPIS_ORE, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE,
            Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.NETHER_GOLD_ORE, Blocks.ANCIENT_DEBRIS
    );

    private BetterBlockPos min;
    private BetterBlockPos max;
    private List<BetterBlockPos> targets;
    private int currentTargetIndex;
    private LongOpenHashSet minedOres;
    private BlockOptionalMetaLookup oreFilter;
    private int ticksSinceOreScan;
    private BetterBlockPos oreTarget;
    private BetterBlockPos lastOreTarget;
    private int oreAttempts;

    public BranchMineProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public boolean isActive() {
        return targets != null && !targets.isEmpty() && currentTargetIndex < targets.size();
    }

    @Override
    public void branchMine(BlockPos pos1, BlockPos pos2) {
        this.min = new BetterBlockPos(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ())
        );
        this.max = new BetterBlockPos(
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ())
        );
        this.currentTargetIndex = 0;
        this.minedOres = new LongOpenHashSet();
        this.oreFilter = new BlockOptionalMetaLookup(DEFAULT_ORES.toArray(new Block[0]));
        this.ticksSinceOreScan = 0;
        this.oreTarget = null;
        this.lastOreTarget = null;
        this.oreAttempts = 0;
        generatePattern();
        if (targets.isEmpty()) {
            logDirect("Branch mine area is too small");
            onLostControl();
        } else {
            logDirect("Branch mining " + targets.size() + " waypoints");
        }
    }

    @Override
    public void branchMine(ISelection selection) {
        branchMine(selection.pos1(), selection.pos2());
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (targets == null || targets.isEmpty() || currentTargetIndex >= targets.size()) {
            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }

        boolean patternCalcFailed = calcFailed;

        // Handle ore collection
        if (Baritone.settings().branchMineOreCollection.value && oreTarget != null) {
            // Check if the ore is already gone
            BlockStateInterface bsi = new BlockStateInterface(ctx);
            if (!oreFilter.has(bsi.get0(oreTarget.x, oreTarget.y, oreTarget.z))) {
                minedOres.add(BetterBlockPos.longHash(oreTarget.x, oreTarget.y, oreTarget.z));
                oreTarget = null;
                lastOreTarget = null;
                oreAttempts = 0;
                // Vein mining: scan a larger radius for adjacent ores before returning to pattern
                scanForOres(4);
                if (oreTarget != null) {
                    return new PathingCommand(new GoalTwoBlocks(oreTarget), PathingCommandType.SET_GOAL_AND_PATH);
                }
            } else {
                if (calcFailed) {
                    minedOres.add(BetterBlockPos.longHash(oreTarget.x, oreTarget.y, oreTarget.z));
                    oreTarget = null;
                    lastOreTarget = null;
                    oreAttempts = 0;
                    patternCalcFailed = false;
                } else {
                    // Timeout check
                    if (!oreTarget.equals(lastOreTarget)) {
                        lastOreTarget = oreTarget;
                        oreAttempts = 0;
                    }
                    oreAttempts++;
                    if (oreAttempts > 120) {
                        logDirect("Giving up on ore at " + oreTarget);
                        minedOres.add(BetterBlockPos.longHash(oreTarget.x, oreTarget.y, oreTarget.z));
                        oreTarget = null;
                        lastOreTarget = null;
                        oreAttempts = 0;
                    } else if (isSafeToCancel) {
                        // Try to actively mine the ore if we can reach it
                        Optional<Rotation> rot = RotationUtils.reachable(ctx, oreTarget);
                        if (rot.isPresent()) {
                            baritone.getLookBehavior().updateTarget(rot.get(), true);
                            MovementHelper.switchToBestToolFor(ctx, ctx.world().getBlockState(oreTarget));
                            if (ctx.isLookingAt(oreTarget) || ctx.playerRotations().isReallyCloseTo(rot.get())) {
                                baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, true);
                                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
                            }
                        }
                        // Not reachable yet, path closer
                        return new PathingCommand(new GoalTwoBlocks(oreTarget), PathingCommandType.SET_GOAL_AND_PATH);
                    } else {
                        // Not safe to cancel, just keep pathing
                        return new PathingCommand(new GoalTwoBlocks(oreTarget), PathingCommandType.SET_GOAL_AND_PATH);
                    }
                }
            }
        }

        // Scan for ores periodically (only when not already chasing one)
        if (Baritone.settings().branchMineOreCollection.value && oreTarget == null) {
            ticksSinceOreScan++;
            if (ticksSinceOreScan >= Baritone.settings().branchMineScanInterval.value) {
                ticksSinceOreScan = 0;
                scanForOres();
            }
            if (oreTarget != null) {
                return new PathingCommand(new GoalTwoBlocks(oreTarget), PathingCommandType.SET_GOAL_AND_PATH);
            }
        }

        // Pattern target handling
        if (patternCalcFailed) {
            logDirect("Failed to path to branch mine target, skipping");
            currentTargetIndex++;
            if (currentTargetIndex >= targets.size()) {
                onLostControl();
                logDirect("Branch mining complete");
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
        }

        BetterBlockPos target = targets.get(currentTargetIndex);
        if (hasArrived(target)) {
            currentTargetIndex++;
            if (currentTargetIndex >= targets.size()) {
                onLostControl();
                logDirect("Branch mining complete");
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
            target = targets.get(currentTargetIndex);
        }

        return new PathingCommand(new GoalBlock(target), PathingCommandType.SET_GOAL_AND_PATH);
    }

    @Override
    public void onLostControl() {
        min = null;
        max = null;
        targets = null;
        currentTargetIndex = 0;
        minedOres = null;
        oreFilter = null;
        ticksSinceOreScan = 0;
        oreTarget = null;
        lastOreTarget = null;
        oreAttempts = 0;
    }

    private void scanForOres() {
        scanForOres(2);
    }

    private void scanForOres(int radius) {
        if (oreFilter == null) {
            return;
        }
        BetterBlockPos center = ctx.playerFeet();
        BlockStateInterface bsi = new BlockStateInterface(ctx);
        BetterBlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BetterBlockPos pos = new BetterBlockPos(center.x + dx, center.y + dy, center.z + dz);
                    long hash = BetterBlockPos.longHash(pos.x, pos.y, pos.z);
                    if (minedOres.contains(hash)) {
                        continue;
                    }
                    if (oreFilter.has(bsi.get0(pos.x, pos.y, pos.z))) {
                        if (!isExposedToAir(bsi, pos)) {
                            continue;
                        }
                        double dist = center.distanceSq(pos);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = pos;
                        }
                    }
                }
            }
        }
        if (closest != null) {
            oreTarget = closest;
            logDirect("Found exposed ore at " + closest + ", collecting");
        }
    }

    private boolean isExposedToAir(BlockStateInterface bsi, BetterBlockPos pos) {
        return bsi.get0(pos.x + 1, pos.y, pos.z).isAir()
                || bsi.get0(pos.x - 1, pos.y, pos.z).isAir()
                || bsi.get0(pos.x, pos.y + 1, pos.z).isAir()
                || bsi.get0(pos.x, pos.y - 1, pos.z).isAir()
                || bsi.get0(pos.x, pos.y, pos.z + 1).isAir()
                || bsi.get0(pos.x, pos.y, pos.z - 1).isAir();
    }

    private boolean hasArrived(BetterBlockPos target) {
        BetterBlockPos feet = ctx.playerFeet();
        return Math.abs(feet.x - target.x) <= 1
                && Math.abs(feet.y - target.y) <= 1
                && Math.abs(feet.z - target.z) <= 1;
    }

    private void generatePattern() {
        targets = new ArrayList<>();
        int interval = Baritone.settings().branchMineInterval.value;
        int tunnelHeight = Baritone.settings().branchMineTunnelHeight.value;
        int layerHeight = Baritone.settings().branchMineLayerHeight.value;
        boolean staircase = Baritone.settings().branchMineStaircase.value;
        int staggerOffset = Baritone.settings().branchMineStaggerOffset.value;

        int sizeX = max.x - min.x + 1;
        int sizeZ = max.z - min.z + 1;

        boolean mainAxisX = sizeX >= sizeZ;

        int numLayers = Math.max(1, (max.y - min.y + 1) / layerHeight);
        int startY = Math.max(min.y, max.y - tunnelHeight + 1);

        for (int layer = 0; layer < numLayers; layer++) {
            int y = startY - layer * layerHeight;
            if (y < min.y) {
                break;
            }

            boolean reverse = (layer % 2) != 0;
            int stagger = (layer * staggerOffset) % interval;

            if (mainAxisX) {
                generateLayerX(y, interval, reverse, stagger);
            } else {
                generateLayerZ(y, interval, reverse, stagger);
            }

            if (staircase && layer < numLayers - 1 && y - layerHeight >= min.y) {
                addStaircase(mainAxisX, reverse, y, layerHeight);
            }
        }
    }

    private void generateLayerX(int y, int interval, boolean reverse, int stagger) {
        boolean reverseX = reverse;

        for (int z = min.z + stagger; z <= max.z; z += interval) {
            int startX = reverseX ? max.x : min.x;
            int endX = reverseX ? min.x : max.x;
            int stepX = reverseX ? -1 : 1;

            // Main tunnel: waypoint every 2 blocks
            for (int x = startX; stepX > 0 ? x < endX : x > endX; x += stepX * 2) {
                targets.add(new BetterBlockPos(x, y, z));
            }
            targets.add(new BetterBlockPos(endX, y, z));

            // Connect to next lane: every single block
            if (z + interval <= max.z) {
                int nextZ = Math.min(z + interval, max.z);
                for (int bz = z + 1; bz <= nextZ; bz++) {
                    targets.add(new BetterBlockPos(endX, y, bz));
                }
            }

            reverseX = !reverseX;
        }
    }

    private void generateLayerZ(int y, int interval, boolean reverse, int stagger) {
        boolean reverseZ = reverse;

        for (int x = min.x + stagger; x <= max.x; x += interval) {
            int startZ = reverseZ ? max.z : min.z;
            int endZ = reverseZ ? min.z : max.z;
            int stepZ = reverseZ ? -1 : 1;

            // Main tunnel: waypoint every 2 blocks
            for (int z = startZ; stepZ > 0 ? z < endZ : z > endZ; z += stepZ * 2) {
                targets.add(new BetterBlockPos(x, y, z));
            }
            targets.add(new BetterBlockPos(x, y, endZ));

            // Connect to next lane: every single block
            if (x + interval <= max.x) {
                int nextX = Math.min(x + interval, max.x);
                for (int bx = x + 1; bx <= nextX; bx++) {
                    targets.add(new BetterBlockPos(bx, y, endZ));
                }
            }

            reverseZ = !reverseZ;
        }
    }

    private void addStaircase(boolean mainAxisX, boolean reverse, int y, int layerHeight) {
        BetterBlockPos last = targets.get(targets.size() - 1);
        int step = reverse ? -1 : 1;
        int drops = layerHeight;

        if (mainAxisX) {
            for (int i = 0; i < drops; i++) {
                targets.add(new BetterBlockPos(last.x + step, last.y - 1, last.z));
                last = targets.get(targets.size() - 1);
            }
        } else {
            for (int i = 0; i < drops; i++) {
                targets.add(new BetterBlockPos(last.x, last.y - 1, last.z + step));
                last = targets.get(targets.size() - 1);
            }
        }
    }

    @Override
    public String displayName0() {
        if (targets == null || targets.isEmpty() || currentTargetIndex >= targets.size()) {
            return "Branch mining (inactive)";
        }
        return "Branch mining " + currentTargetIndex + "/" + targets.size();
    }
}
