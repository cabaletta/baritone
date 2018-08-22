/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.pathing.movement.movements;

import baritone.Baritone;
import baritone.behavior.impl.LookBehaviorUtils;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.pathing.movement.MovementState.MovementStatus;
import baritone.pathing.movement.MovementState.MovementTarget;
import baritone.utils.BlockStateInterface;
import baritone.utils.InputOverrideHandler;
import baritone.utils.Rotation;
import baritone.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class MovementFall extends Movement {

    private static final ItemStack STACK_BUCKET_WATER = new ItemStack(Items.WATER_BUCKET);
    private static final ItemStack STACK_BUCKET_EMPTY = new ItemStack(Items.BUCKET);

    public MovementFall(BlockPos src, BlockPos dest) {
        super(src, dest, MovementFall.buildPositionsToBreak(src, dest), new BlockPos[]{dest.down()});
    }

    @Override
    protected double calculateCost(CalculationContext context) {
        if (!MovementHelper.canWalkOn(positionsToPlace[0])) {
            return COST_INF;
        }
        double placeBucketCost = 0.0;
        if (!BlockStateInterface.isWater(dest) && src.getY() - dest.getY() > context.maxFallHeightNoWater()) {
            if (!context.hasWaterBucket()) {
                return COST_INF;
            }
            if (src.getY() - dest.getY() > context.maxFallHeightBucket()) {
                return COST_INF;
            }
            placeBucketCost = context.placeBlockCost();
        }
        double frontTwo = MovementHelper.getMiningDurationTicks(context, positionsToBreak[0]) + MovementHelper.getMiningDurationTicks(context, positionsToBreak[1]);
        if (frontTwo >= COST_INF) {
            return COST_INF;
        }
        for (int i = 2; i < positionsToBreak.length; i++) {
            // TODO is this the right check here?
            // miningDurationTicks is all right, but shouldn't it be canWalkThrough instead?
            // lilypads (i think?) are 0 ticks to mine, but they definitely cause fall damage
            // same thing for falling through water... we can't actually do that
            // and falling through signs is possible, but they do have a mining duration, right?
            if (MovementHelper.getMiningDurationTicks(context, positionsToBreak[i]) > 0) {
                //can't break while falling
                return COST_INF;
            }
        }
        return WALK_OFF_BLOCK_COST + FALL_N_BLOCKS_COST[positionsToBreak.length - 1] + placeBucketCost + frontTwo;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        switch (state.getStatus()) {
            case WAITING:
                state.setStatus(MovementStatus.RUNNING);
            case RUNNING:
                break;
            default:
                return state;
        }
        BlockPos playerFeet = playerFeet();
        Optional<Rotation> targetRotation = Optional.empty();
        if (!BlockStateInterface.isWater(dest) && src.getY() - dest.getY() > Baritone.settings().maxFallHeightNoWater.get() && !playerFeet.equals(dest)) {
            if (!player().inventory.hasItemStack(STACK_BUCKET_WATER) || world().provider.isNether()) { // TODO check if water bucket is on hotbar or main inventory
                state.setStatus(MovementStatus.UNREACHABLE);
                return state;
            }
            if (player().posY - dest.getY() < mc.playerController.getBlockReachDistance()) {
                player().inventory.currentItem = player().inventory.getSlotFor(STACK_BUCKET_WATER);
                targetRotation = LookBehaviorUtils.reachable((BlockStateInterface.get(dest).getCollisionBoundingBox(mc.world, dest) == Block.NULL_AABB) ? dest : dest.down());
            }
        }
        if (targetRotation.isPresent()) {
            state.setInput(InputOverrideHandler.Input.CLICK_RIGHT, true)
                    .setTarget(new MovementTarget(targetRotation.get(), true));
        } else {
            state.setTarget(new MovementTarget(Utils.calcRotationFromVec3d(playerHead(), Utils.getBlockPosCenter(dest)), false));
        }
        if (playerFeet.equals(dest) && (player().posY - playerFeet.getY() < 0.094 // lilypads
                || BlockStateInterface.isWater(dest))) {
            if (BlockStateInterface.isWater(dest) && player().inventory.hasItemStack(STACK_BUCKET_EMPTY)) {
                player().inventory.currentItem = player().inventory.getSlotFor(STACK_BUCKET_EMPTY);
                if (player().motionY >= 0) {
                    return state.setInput(InputOverrideHandler.Input.CLICK_RIGHT, true);
                } else {
                    return state;
                }
            }
            return state.setStatus(MovementStatus.SUCCESS);
        }
        Vec3d destCenter = Utils.getBlockPosCenter(dest); // we are moving to the 0.5 center not the edge (like if we were falling on a ladder)
        if (Math.abs(player().posX - destCenter.x) > 0.2 || Math.abs(player().posZ - destCenter.z) > 0.2) {
            state.setInput(InputOverrideHandler.Input.MOVE_FORWARD, true);
        }
        return state;
    }

    private static BlockPos[] buildPositionsToBreak(BlockPos src, BlockPos dest) {
        BlockPos[] toBreak;
        int diffX = src.getX() - dest.getX();
        int diffZ = src.getZ() - dest.getZ();
        int diffY = src.getY() - dest.getY();
        toBreak = new BlockPos[diffY + 2];
        for (int i = 0; i < toBreak.length; i++) {
            toBreak[i] = new BlockPos(src.getX() - diffX, src.getY() + 1 - i, src.getZ() - diffZ);
        }
        return toBreak;
    }
}
