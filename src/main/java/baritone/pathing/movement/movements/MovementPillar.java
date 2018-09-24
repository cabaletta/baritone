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

import baritone.api.utils.Rotation;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.BlockStateInterface;
import baritone.utils.InputOverrideHandler;
import baritone.utils.Utils;
import baritone.utils.pathing.BetterBlockPos;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class MovementPillar extends Movement {
    private int numTicks = 0;

    public MovementPillar(BetterBlockPos start, BetterBlockPos end) {
        super(start, end, new BetterBlockPos[]{start.up(2)}, start);
    }

    @Override
    public void reset() {
        super.reset();
        numTicks = 0;
    }

    @Override
    protected double calculateCost(CalculationContext context) {
        return cost(context, src.x, src.y, src.z);
    }

    public static double cost(CalculationContext context, int x, int y, int z) {
        Block fromDown = BlockStateInterface.get(x, y, z).getBlock();
        boolean ladder = fromDown instanceof BlockLadder || fromDown instanceof BlockVine;
        IBlockState fromDownDown = BlockStateInterface.get(x, y - 1, z);
        if (!ladder) {
            if (fromDownDown.getBlock() instanceof BlockLadder || fromDownDown.getBlock() instanceof BlockVine) {
                return COST_INF;
            }
            if (fromDownDown.getBlock() instanceof BlockSlab) {
                if (!((BlockSlab) fromDownDown.getBlock()).isDouble() && fromDownDown.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM) {
                    return COST_INF; // can't pillar up from a bottom slab onto a non ladder
                }
            }
        }
        if (!context.hasThrowaway() && !ladder) {
            return COST_INF;
        }
        if (fromDown instanceof BlockVine) {
            if (!hasAgainst(x, y, z)) {
                return COST_INF;
            }
        }
        IBlockState toBreak = BlockStateInterface.get(x, y + 2, z);
        Block toBreakBlock = toBreak.getBlock();
        if (toBreakBlock instanceof BlockFenceGate) {
            return COST_INF;
        }
        Block srcUp = null;
        if (BlockStateInterface.isWater(toBreakBlock) && BlockStateInterface.isWater(fromDown)) {
            srcUp = BlockStateInterface.get(x, y + 1, z).getBlock();
            if (BlockStateInterface.isWater(srcUp)) {
                return LADDER_UP_ONE_COST;
            }
        }
        double hardness = MovementHelper.getMiningDurationTicks(context, x, y + 2, z, toBreak, true);
        if (hardness >= COST_INF) {
            return COST_INF;
        }
        if (hardness != 0) {
            if (toBreakBlock instanceof BlockLadder || toBreakBlock instanceof BlockVine) {
                hardness = 0; // we won't actually need to break the ladder / vine because we're going to use it
            } else {
                IBlockState check = BlockStateInterface.get(x, y + 3, z);
                if (check.getBlock() instanceof BlockFalling) {
                    // see MovementAscend's identical check for breaking a falling block above our head
                    if (srcUp == null) {
                        srcUp = BlockStateInterface.get(x, y + 1, z).getBlock();
                    }
                    if (!(toBreakBlock instanceof BlockFalling) || !(srcUp instanceof BlockFalling)) {
                        return COST_INF;
                    }
                }
                // this is commented because it may have had a purpose, but it's very unclear what it was. it's from the minebot era.
                //if (!MovementHelper.canWalkOn(chkPos, check) || MovementHelper.canWalkThrough(chkPos, check)) {//if the block above where we want to break is not a full block, don't do it
                // TODO why does canWalkThrough mean this action is COST_INF?
                // BlockFalling makes sense, and !canWalkOn deals with weird cases like if it were lava
                // but I don't understand why canWalkThrough makes it impossible
                //    return COST_INF;
                //}
            }
        }
        if (fromDown instanceof BlockLiquid || fromDownDown.getBlock() instanceof BlockLiquid) {//can't pillar on water or in water
            return COST_INF;
        }
        if (ladder) {
            return LADDER_UP_ONE_COST + hardness * 5;
        } else {
            return JUMP_ONE_BLOCK_COST + context.placeBlockCost() + hardness;
        }
    }

    public static boolean hasAgainst(int x, int y, int z) {
        return BlockStateInterface.get(x + 1, y, z).isBlockNormalCube() ||
                BlockStateInterface.get(x - 1, y, z).isBlockNormalCube() ||
                BlockStateInterface.get(x, y, z + 1).isBlockNormalCube() ||
                BlockStateInterface.get(x, y, z - 1).isBlockNormalCube();
    }

    public static BlockPos getAgainst(BlockPos vine) {
        if (BlockStateInterface.get(vine.north()).isBlockNormalCube()) {
            return vine.north();
        }
        if (BlockStateInterface.get(vine.south()).isBlockNormalCube()) {
            return vine.south();
        }
        if (BlockStateInterface.get(vine.east()).isBlockNormalCube()) {
            return vine.east();
        }
        if (BlockStateInterface.get(vine.west()).isBlockNormalCube()) {
            return vine.west();
        }
        return null;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementState.MovementStatus.RUNNING) {
            return state;
        }

        IBlockState fromDown = BlockStateInterface.get(src);
        if (BlockStateInterface.isWater(fromDown.getBlock()) && BlockStateInterface.isWater(dest)) {
            // stay centered while swimming up a water column
            state.setTarget(new MovementState.MovementTarget(Utils.calcRotationFromVec3d(playerHead(), Utils.getBlockPosCenter(dest)), false));
            Vec3d destCenter = Utils.getBlockPosCenter(dest);
            if (Math.abs(player().posX - destCenter.x) > 0.2 || Math.abs(player().posZ - destCenter.z) > 0.2) {
                state.setInput(InputOverrideHandler.Input.MOVE_FORWARD, true);
            }
            if (playerFeet().equals(dest)) {
                return state.setStatus(MovementState.MovementStatus.SUCCESS);
            }
            return state;
        }
        boolean ladder = fromDown.getBlock() instanceof BlockLadder || fromDown.getBlock() instanceof BlockVine;
        boolean vine = fromDown.getBlock() instanceof BlockVine;
        if (!ladder) {
            state.setTarget(new MovementState.MovementTarget(Utils.calcRotationFromVec3d(mc.player.getPositionEyes(1.0F),
                    Utils.getBlockPosCenter(positionToPlace),
                    new Rotation(mc.player.rotationYaw, mc.player.rotationPitch)), true));
        }

        boolean blockIsThere = MovementHelper.canWalkOn(src) || ladder;
        if (ladder) {
            BlockPos against = vine ? getAgainst(src) : src.offset(fromDown.getValue(BlockLadder.FACING).getOpposite());
            if (against == null) {
                logDebug("Unable to climb vines");
                return state.setStatus(MovementState.MovementStatus.UNREACHABLE);
            }

            if (playerFeet().equals(against.up()) || playerFeet().equals(dest)) {
                return state.setStatus(MovementState.MovementStatus.SUCCESS);
            }
            if (MovementHelper.isBottomSlab(src.down())) {
                state.setInput(InputOverrideHandler.Input.JUMP, true);
            }
            /*
            if (thePlayer.getPosition0().getX() != from.getX() || thePlayer.getPosition0().getZ() != from.getZ()) {
                Baritone.moveTowardsBlock(from);
            }
             */

            MovementHelper.moveTowards(state, against);
            return state;
        } else {
            // Get ready to place a throwaway block
            if (!MovementHelper.throwaway(true)) {
                return state.setStatus(MovementState.MovementStatus.UNREACHABLE);
            }

            numTicks++;
            // If our Y coordinate is above our goal, stop jumping
            state.setInput(InputOverrideHandler.Input.JUMP, player().posY < dest.getY());
            state.setInput(InputOverrideHandler.Input.SNEAK, true);

            // Otherwise jump
            if (numTicks > 20) {
                double diffX = player().posX - (dest.getX() + 0.5);
                double diffZ = player().posZ - (dest.getZ() + 0.5);
                double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
                if (dist > 0.17) {//why 0.17? because it seemed like a good number, that's why
                    //[explanation added after baritone port lol] also because it needs to be less than 0.2 because of the 0.3 sneak limit
                    //and 0.17 is reasonably less than 0.2

                    // If it's been more than forty ticks of trying to jump and we aren't done yet, go forward, maybe we are stuck
                    state.setInput(InputOverrideHandler.Input.MOVE_FORWARD, true);
                }
            }

            if (!blockIsThere) {
                Block fr = BlockStateInterface.get(src).getBlock();
                if (!(fr instanceof BlockAir || fr.isReplaceable(Minecraft.getMinecraft().world, src))) {
                    state.setInput(InputOverrideHandler.Input.CLICK_LEFT, true);
                    blockIsThere = false;
                } else if (Minecraft.getMinecraft().player.isSneaking()) {
                    state.setInput(InputOverrideHandler.Input.CLICK_RIGHT, true);
                }
            }
        }

        // If we are at our goal and the block below us is placed
        if (playerFeet().equals(dest) && blockIsThere) {
            return state.setStatus(MovementState.MovementStatus.SUCCESS);
        }

        return state;
    }

    @Override
    protected boolean prepared(MovementState state) {
        if (playerFeet().equals(src) || playerFeet().equals(src.down())) {
            Block block = BlockStateInterface.getBlock(src.down());
            if (block == Blocks.LADDER || block == Blocks.VINE) {
                state.setInput(InputOverrideHandler.Input.SNEAK, true);
            }
        }
        if (BlockStateInterface.isWater(dest.up())) {
            return true;
        }
        return super.prepared(state);
    }
}