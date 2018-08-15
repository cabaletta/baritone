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

import baritone.bot.pathing.movement.CalculationContext;
import baritone.bot.pathing.movement.Movement;
import baritone.bot.pathing.movement.MovementHelper;
import baritone.bot.pathing.movement.MovementState;
import baritone.bot.utils.BlockStateInterface;
import baritone.bot.utils.InputOverrideHandler;
import baritone.bot.utils.Rotation;
import baritone.bot.utils.Utils;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;

public class MovementPillar extends Movement {
    private int numTicks = 0;

    public MovementPillar(BlockPos start, BlockPos end) {
        super(start, end, new BlockPos[]{start.up(2)}, new BlockPos[]{start});
    }

    @Override
    protected double calculateCost(CalculationContext context) {
        Block fromDown = BlockStateInterface.get(src).getBlock();
        boolean ladder = fromDown instanceof BlockLadder || fromDown instanceof BlockVine;
        Block fromDownDown = BlockStateInterface.get(src.down()).getBlock();
        if (!ladder) {
            if (fromDownDown instanceof BlockLadder || fromDownDown instanceof BlockVine) {
                return COST_INF;
            }
        }
        if (!context.hasThrowaway() && !ladder) {
            return COST_INF;
        }
        if (fromDown instanceof BlockVine) {
            if (getAgainst(src) == null) {
                return COST_INF;
            }
        }
        double hardness = getTotalHardnessOfBlocksToBreak(context.getToolSet());
        if (hardness != 0) {
            Block tmp = BlockStateInterface.get(src.up(2)).getBlock();
            if (tmp instanceof BlockLadder || tmp instanceof BlockVine) {
                hardness = 0; // we won't actually need to break the ladder / vine because we're going to use it
            } else {
                BlockPos chkPos = src.up(3);
                IBlockState check = BlockStateInterface.get(chkPos);
                if (!MovementHelper.canWalkOn(chkPos, check) || MovementHelper.canWalkThrough(chkPos, check) || check.getBlock() instanceof BlockFalling) {//if the block above where we want to break is not a full block, don't do it
                    return COST_INF;
                }
            }
        }
        if (fromDown instanceof BlockLiquid || fromDownDown instanceof BlockLiquid) {//can't pillar on water or in water
            return COST_INF;
        }
        if (ladder) {
            return LADDER_UP_ONE_COST + hardness;
        } else {
            return JUMP_ONE_BLOCK_COST + PLACE_ONE_BLOCK_COST + hardness;
        }
    }

    public static BlockPos getAgainst(BlockPos vine) {
        if (BlockStateInterface.get(vine.north()).isBlockNormalCube()) {
            return vine.north();
        }
        if (BlockStateInterface.get(vine.south()).isBlockNormalCube()) {
            return vine.north();
        }
        if (BlockStateInterface.get(vine.east()).isBlockNormalCube()) {
            return vine.north();
        }
        if (BlockStateInterface.get(vine.west()).isBlockNormalCube()) {
            return vine.north();
        }
        return null;
    }

    @Override
    public void onRunning(MovementState state) {
        IBlockState fromDown = BlockStateInterface.get(src);
        boolean ladder = fromDown.getBlock() instanceof BlockLadder || fromDown.getBlock() instanceof BlockVine;
        boolean vine = fromDown.getBlock() instanceof BlockVine;
        if (!ladder) {
            state.setTarget(new MovementState.MovementTarget(Utils.calcRotationFromVec3d(mc.player.getPositionEyes(1.0F),
                    Utils.getBlockPosCenter(positionsToPlace[0]),
                    new Rotation(mc.player.rotationYaw, mc.player.rotationPitch))));
        }
        EntityPlayerSP thePlayer = Minecraft.getMinecraft().player;
        boolean blockIsThere = MovementHelper.canWalkOn(src) || ladder;
        if (ladder) {
            BlockPos against = vine ? getAgainst(src) : src.offset(fromDown.getValue(BlockLadder.FACING).getOpposite());
            if (against == null) {
                displayChatMessageRaw("Unable to climb vines");
                state.setStatus(MovementState.MovementStatus.UNREACHABLE);
                return;
            }
            if (playerFeet().equals(against.up()) || playerFeet().equals(dest)) {
                state.setStatus(MovementState.MovementStatus.SUCCESS);
                return;
            }
            /*if (thePlayer.getPosition0().getX() != from.getX() || thePlayer.getPosition0().getZ() != from.getZ()) {
             Baritone.moveTowardsBlock(from);
             }*/
            MovementHelper.moveTowards(state, against);
            return;
        } else {
            if (!MovementHelper.throwaway(true)) {//get ready to place a throwaway block
                state.setStatus(MovementState.MovementStatus.UNREACHABLE);
                return;
            }
            numTicks++;
            state.setInput(InputOverrideHandler.Input.JUMP, thePlayer.posY < dest.getY()); //if our Y coordinate is above our goal, stop jumping
            state.setInput(InputOverrideHandler.Input.SNEAK, true);
            //otherwise jump
            if (numTicks > 40) {
                double diffX = thePlayer.posX - (dest.getX() + 0.5);
                double diffZ = thePlayer.posZ - (dest.getZ() + 0.5);
                double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
                if (dist > 0.17) {//why 0.17? because it seemed like a good number, that's why
                    //[explanation added after baritone port lol] also because it needs to be less than 0.2 because of the 0.3 sneak limit
                    //and 0.17 is reasonably less than 0.2
                    state.setInput(InputOverrideHandler.Input.MOVE_FORWARD, true);//if it's been more than forty ticks of trying to jump and we aren't done yet, go forward, maybe we are stuck
                }
            }
            if (!blockIsThere) {
                Block fr = BlockStateInterface.get(src).getBlock();
                if (!(fr instanceof BlockAir || fr.isReplaceable(Minecraft.getMinecraft().world, src))) {
                    state.setInput(InputOverrideHandler.Input.CLICK_LEFT, true);
                    blockIsThere = false;
                } else if (Minecraft.getMinecraft().player.isSneaking()) {
                    state.setInput(InputOverrideHandler.Input.CLICK_RIGHT, true);//constantly right click
                }
            }
        }
        if (playerFeet().equals(dest) && blockIsThere) {//if we are at our goal and the block below us is placed
            state.setStatus(MovementState.MovementStatus.SUCCESS);
        }
    }
}