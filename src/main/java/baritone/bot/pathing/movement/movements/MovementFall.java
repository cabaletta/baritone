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

package baritone.bot.pathing.movement.movements;

import baritone.bot.InputOverrideHandler;
import baritone.bot.behavior.impl.LookBehaviorUtils;
import baritone.bot.pathing.movement.*;
import baritone.bot.pathing.movement.MovementState.MovementStatus;
import baritone.bot.pathing.movement.MovementState.MovementTarget;
import baritone.bot.utils.BlockStateInterface;
import baritone.bot.utils.Rotation;
import baritone.bot.utils.Utils;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class MovementFall extends Movement {

    private static final ItemStack STACK_BUCKET_WATER = new ItemStack(Items.WATER_BUCKET);
    private static final ItemStack STACK_BUCKET_AIR = new ItemStack(Items.BUCKET);

    public MovementFall(BlockPos src, BlockPos dest) {
        super(src, dest, MovementFall.buildPositionsToBreak(src, dest), new BlockPos[] { dest.down() });
    }

    @Override
    protected double calculateCost(CalculationContext context) {
        if (!MovementHelper.canWalkOn(positionsToPlace[0])) {
            return COST_INF;
        }
        double placeBucketCost = 0.0;
        if (!BlockStateInterface.isWater(dest) && src.getY() - dest.getY() > 3) {
            placeBucketCost = ActionCosts.PLACE_ONE_BLOCK_COST;
        }
        double cost = getTotalHardnessOfBlocksToBreak(context.getToolSet());
        if (cost != 0) {
            return COST_INF;
        }
        return WALK_OFF_BLOCK_COST + FALL_N_BLOCKS_COST[positionsToBreak.length - 1] + cost + placeBucketCost;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);

        switch (state.getStatus()) {
            case PREPPING:
            case UNREACHABLE:
            case FAILED:
                return state;
            case WAITING:
                state.setStatus(MovementStatus.RUNNING);
            case RUNNING:
                BlockPos playerFeet = playerFeet();
                if (!BlockStateInterface.isWater(dest) && playerFeet().getY() - dest.getY() > 3) {
                    if (!player().inventory.hasItemStack(STACK_BUCKET_WATER) || world().provider.isNether()) {
                        state.setStatus(MovementStatus.UNREACHABLE);
                        return state;
                    }
                    player().inventory.currentItem = player().inventory.getSlotFor(STACK_BUCKET_WATER);
                    LookBehaviorUtils.reachable(dest).ifPresent(rotation ->
                            state.setInput(InputOverrideHandler.Input.CLICK_RIGHT, true)
                                    .setTarget(new MovementTarget(rotation))
                    );
                } else {
                    Rotation rotationToBlock = Utils.calcRotationFromVec3d(playerHead(), Utils.calcCenterFromCoords(dest, world()));
                    state.setTarget(new MovementTarget(rotationToBlock));
                }
                if (playerFeet.equals(dest) && (player().posY - playerFeet.getY() < 0.01
                        || (BlockStateInterface.isWater(dest) && !player().inventory.hasItemStack(STACK_BUCKET_AIR)))) {
                    if (BlockStateInterface.isWater(dest) && player().inventory.hasItemStack(STACK_BUCKET_AIR)) {
                        return state.setInput(InputOverrideHandler.Input.CLICK_RIGHT, true);
                    }
                    return state.setStatus(MovementStatus.SUCCESS);
                }
                Vec3d destCenter = Utils.calcCenterFromCoords(dest, world());
                if (Math.abs(player().posX - destCenter.x) > 0.2 || Math.abs(player().posZ - destCenter.z) > 0.2) {
                    state.setInput(InputOverrideHandler.Input.MOVE_FORWARD, true);
                }
                return state;
            default:
                return state;
        }
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
