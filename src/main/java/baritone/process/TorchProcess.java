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
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Places torches on dark blocks near the player, so mobs don't spawn in the tunnels we dig.
 * <p>
 * Spacing comes from the light level itself: once a torch is placed, everything it lights up
 * stops being a candidate.
 */
public final class TorchProcess extends BaritoneProcessHelper {

    /**
     * Block light updates lag a tick or two behind the placement, so remember where we just placed
     * to avoid stacking torches in the same spot.
     */
    private static final long PLACEMENT_COOLDOWN = 20;

    private final Map<BlockPos, Long> recentlyPlaced = new HashMap<>();

    private long lastPlacement = -PLACEMENT_COOLDOWN;

    /**
     * isActive can run several times a tick and onTick needs the same result, so scan once per tick.
     */
    private List<BlockPos> darkSpots = Collections.emptyList();
    private Level scannedWorld;
    private long scannedTime;

    public TorchProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public boolean isActive() {
        if (ctx.player() == null || ctx.world() == null) {
            return false;
        }
        if (!Baritone.settings().autoTorch.value) {
            return false;
        }
        long now = ctx.world().getGameTime();
        recentlyPlaced.values().removeIf(time -> now - time > PLACEMENT_COOLDOWN);
        if (now - lastPlacement < PLACEMENT_COOLDOWN) {
            return false;
        }
        if (!selectTorch(false)) {
            return false;
        }
        return !darkSpots().isEmpty();
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (!isSafeToCancel) {
            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }
        baritone.getInputOverrideHandler().clearAllKeys();
        for (BlockPos pos : darkSpots()) {
            MovementState fake = new MovementState();
            switch (MovementHelper.attemptToPlaceABlock(fake, baritone, pos, false, false, this::selectTorch)) {
                case NO_OPTION:
                    continue;
                case READY_TO_PLACE:
                    baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
                    long now = ctx.world().getGameTime();
                    recentlyPlaced.put(pos, now);
                    lastPlacement = now;
                    return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
                case ATTEMPTING:
                    baritone.getLookBehavior().updateTarget(fake.getTarget().getRotation().get(), true);
                    return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
                default:
                    throw new IllegalStateException();
            }
        }
        return new PathingCommand(null, PathingCommandType.DEFER); // cede to other processes
    }

    private List<BlockPos> darkSpots() {
        long now = ctx.world().getGameTime();
        if (ctx.world() != scannedWorld || now != scannedTime) {
            darkSpots = scanDarkSpots();
            scannedWorld = ctx.world();
            scannedTime = now;
        }
        return darkSpots;
    }

    /**
     * Air blocks within reach that are dark enough to want a torch and have something to hold one.
     */
    private List<BlockPos> scanDarkSpots() {
        double reach = ctx.playerController().getBlockReachDistance();
        int radius = (int) Math.ceil(reach);
        Vec3 eyes = ctx.playerHead();
        BetterBlockPos feet = ctx.playerFeet();
        List<BlockPos> spots = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos pos = feet.offset(dx, dy, dz);
                    if (eyes.distanceToSqr(Vec3.atCenterOf(pos)) <= reach * reach && wantsTorch(pos)) {
                        spots.add(pos);
                    }
                }
            }
        }
        spots.sort(Comparator.comparingDouble(feet::distSqr));
        return spots;
    }

    private boolean wantsTorch(BlockPos pos) {
        if (recentlyPlaced.containsKey(pos)) {
            return false;
        }
        if (!ctx.world().getBlockState(pos).isAir()) {
            return false;
        }
        if (ctx.world().getBrightness(LightLayer.BLOCK, pos) > Baritone.settings().autoTorchLightLevel.value) {
            return false;
        }
        if (Blocks.TORCH.defaultBlockState().canSurvive(ctx.world(), pos)) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (WallTorchBlock.canSurvive(ctx.world(), pos, direction)) {
                return true;
            }
        }
        return false;
    }

    private boolean selectTorch(boolean select) {
        return baritone.getInventoryBehavior().throwaway(select, TorchProcess::isTorch);
    }

    private static boolean isTorch(ItemStack stack) {
        return stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH);
    }

    @Override
    public void onLostControl() {
        recentlyPlaced.clear();
    }

    @Override
    public String displayName0() {
        return "Placing torches";
    }

    @Override
    public boolean isTemporary() {
        return true;
    }

    @Override
    public double priority() {
        return 5;
    }
}
