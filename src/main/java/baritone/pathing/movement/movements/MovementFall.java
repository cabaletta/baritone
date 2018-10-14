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

package baritone.pathing.movement.movements;

import baritone.Baritone;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.*;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.pathing.movement.MovementState.MovementTarget;
import baritone.utils.BlockStateInterface;
import baritone.utils.InputOverrideHandler;
import baritone.utils.pathing.MutableMoveResult;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public class MovementFall extends Movement {

    private static final ItemStack STACK_BUCKET_WATER = new ItemStack(Items.WATER_BUCKET);
    private static final ItemStack STACK_BUCKET_EMPTY = new ItemStack(Items.BUCKET);

    public MovementFall(BetterBlockPos src, BetterBlockPos dest) {
        super(src, dest, MovementFall.buildPositionsToBreak(src, dest));
    }

    @Override
    protected double calculateCost(CalculationContext context) {
        MutableMoveResult result = new MutableMoveResult();
        MovementDescend.cost(context, src.x, src.y, src.z, dest.x, dest.z, result);
        if (result.y != dest.y) {
            return COST_INF; // doesn't apply to us, this position is a descend not a fall
        }
        return result.cost;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        BlockPos playerFeet = playerFeet();
        Rotation targetRotation = null;
        if (!BlockStateInterface.isWater(dest) && src.getY() - dest.getY() > Baritone.settings().maxFallHeightNoWater.get() && !playerFeet.equals(dest)) {
            if (!InventoryPlayer.isHotbar(player().inventory.getSlotFor(STACK_BUCKET_WATER)) || world().provider.isNether()) {
                return state.setStatus(MovementStatus.UNREACHABLE);
            }

            if (player().posY - dest.getY() < mc.playerController.getBlockReachDistance()) {
                player().inventory.currentItem = player().inventory.getSlotFor(STACK_BUCKET_WATER);

                targetRotation = new Rotation(player().rotationYaw, 90.0F);

                RayTraceResult trace = RayTraceUtils.simulateRayTrace(player().rotationYaw, 90.0F);
                if (trace != null && trace.typeOfHit == RayTraceResult.Type.BLOCK) {
                    state.setInput(InputOverrideHandler.Input.CLICK_RIGHT, true);
                }
            }
        }
        if (targetRotation != null) {
            state.setTarget(new MovementTarget(targetRotation, true));
        } else {
            state.setTarget(new MovementTarget(RotationUtils.calcRotationFromVec3d(playerHead(), VecUtils.getBlockPosCenter(dest)), false));
        }
        if (playerFeet.equals(dest) && (player().posY - playerFeet.getY() < 0.094 || BlockStateInterface.isWater(dest))) { // 0.094 because lilypads
            if (BlockStateInterface.isWater(dest)) {
                if (InventoryPlayer.isHotbar(player().inventory.getSlotFor(STACK_BUCKET_EMPTY))) {
                    player().inventory.currentItem = player().inventory.getSlotFor(STACK_BUCKET_EMPTY);
                    if (player().motionY >= 0) {
                        return state.setInput(InputOverrideHandler.Input.CLICK_RIGHT, true);
                    } else {
                        return state;
                    }
                } else {
                    if (player().motionY >= 0) {
                        return state.setStatus(MovementStatus.SUCCESS);
                    } // don't else return state; we need to stay centered because this water might be flowing under the surface
                }
            } else {
                return state.setStatus(MovementStatus.SUCCESS);
            }
        }
        Vec3d destCenter = VecUtils.getBlockPosCenter(dest); // we are moving to the 0.5 center not the edge (like if we were falling on a ladder)
        if (Math.abs(player().posX - destCenter.x) > 0.15 || Math.abs(player().posZ - destCenter.z) > 0.15) {
            state.setInput(InputOverrideHandler.Input.MOVE_FORWARD, true);
        }
        return state;
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        // if we haven't started walking off the edge yet, or if we're in the process of breaking blocks before doing the fall
        // then it's safe to cancel this
        return playerFeet().equals(src) || state.getStatus() != MovementStatus.RUNNING;
    }

    private static BetterBlockPos[] buildPositionsToBreak(BetterBlockPos src, BetterBlockPos dest) {
        BetterBlockPos[] toBreak;
        int diffX = src.getX() - dest.getX();
        int diffZ = src.getZ() - dest.getZ();
        int diffY = src.getY() - dest.getY();
        toBreak = new BetterBlockPos[diffY + 2];
        for (int i = 0; i < toBreak.length; i++) {
            toBreak[i] = new BetterBlockPos(src.getX() - diffX, src.getY() + 1 - i, src.getZ() - diffZ);
        }
        return toBreak;
    }

    @Override
    protected boolean prepared(MovementState state) {
        if (state.getStatus() == MovementStatus.WAITING) {
            return true;
        }
        // only break if one of the first three needs to be broken
        // specifically ignore the last one which might be water
        for (int i = 0; i < 4 && i < positionsToBreak.length; i++) {
            if (!MovementHelper.canWalkThrough(positionsToBreak[i])) {
                return super.prepared(state);
            }
        }
        return true;
    }
}
