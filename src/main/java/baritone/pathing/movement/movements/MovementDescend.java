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
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.pathing.movement.MovementState.MovementStatus;
import baritone.utils.BlockStateInterface;
import baritone.utils.InputOverrideHandler;
import baritone.utils.pathing.BetterBlockPos;
import baritone.utils.pathing.MoveResult;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

import static baritone.utils.pathing.MoveResult.IMPOSSIBLE;

public class MovementDescend extends Movement {

    private int numTicks = 0;

    public MovementDescend(BetterBlockPos start, BetterBlockPos end) {
        super(start, end, new BetterBlockPos[]{end.up(2), end.up(), end}, end.down());
    }

    @Override
    public void reset() {
        super.reset();
        numTicks = 0;
    }

    @Override
    protected double calculateCost(CalculationContext context) {
        MoveResult result = cost(context, src.x, src.y, src.z, dest.x, dest.z);
        if (result.destY != dest.y) {
            return COST_INF; // doesn't apply to us, this position is a fall not a descend
        }
        return result.cost;
    }

    public static MoveResult cost(CalculationContext context, int x, int y, int z, int destX, int destZ) {
        Block fromDown = BlockStateInterface.get(x, y - 1, z).getBlock();
        if (fromDown == Blocks.LADDER || fromDown == Blocks.VINE) {
            return IMPOSSIBLE;
        }

        double totalCost = 0;
        IBlockState destDown = BlockStateInterface.get(destX, y - 1, destZ);
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y - 1, destZ, destDown, false);
        if (totalCost >= COST_INF) {
            return IMPOSSIBLE;
        }
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y, destZ, false);
        if (totalCost >= COST_INF) {
            return IMPOSSIBLE;
        }
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y + 1, destZ, true); // only the top block in the 3 we need to mine needs to consider the falling blocks above
        if (totalCost >= COST_INF) {
            return IMPOSSIBLE;
        }

        // A
        //SA
        // A
        // B
        // C
        // D
        //if S is where you start, B needs to be air for a movementfall
        //A is plausibly breakable by either descend or fall
        //C, D, etc determine the length of the fall

        IBlockState below = BlockStateInterface.get(destX, y - 2, destZ);
        if (!MovementHelper.canWalkOn(destX, y - 2, destZ, below)) {
            return dynamicFallCost(context, x, y, z, destX, destZ, totalCost, below);
        }

        if (destDown.getBlock() == Blocks.LADDER || destDown.getBlock() == Blocks.VINE) {
            return IMPOSSIBLE;
        }

        // we walk half the block plus 0.3 to get to the edge, then we walk the other 0.2 while simultaneously falling (math.max because of how it's in parallel)
        double walk = WALK_OFF_BLOCK_COST;
        if (fromDown == Blocks.SOUL_SAND) {
            // use this ratio to apply the soul sand speed penalty to our 0.8 block distance
            walk = WALK_ONE_OVER_SOUL_SAND_COST;
        }
        totalCost += walk + Math.max(FALL_N_BLOCKS_COST[1], CENTER_AFTER_FALL_COST);
        return new MoveResult(destX, y - 1, destZ, totalCost);
    }

    public static MoveResult dynamicFallCost(CalculationContext context, int x, int y, int z, int destX, int destZ, double frontBreak, IBlockState below) {
        if (frontBreak != 0 && BlockStateInterface.get(destX, y + 2, destZ).getBlock() instanceof BlockFalling) {
            // if frontBreak is 0 we can actually get through this without updating the falling block and making it actually fall
            // but if frontBreak is nonzero, we're breaking blocks in front, so don't let anything fall through this column,
            // and potentially replace the water we're going to fall into
            return IMPOSSIBLE;
        }
        if (!MovementHelper.canWalkThrough(destX, y - 2, destZ, below) && below.getBlock() != Blocks.WATER) {
            return IMPOSSIBLE;
        }
        for (int fallHeight = 3; true; fallHeight++) {
            int newY = y - fallHeight;
            if (newY < 0) {
                // when pathing in the end, where you could plausibly fall into the void
                // this check prevents it from getting the block at y=-1 and crashing
                return IMPOSSIBLE;
            }
            IBlockState ontoBlock = BlockStateInterface.get(destX, newY, destZ);
            double tentativeCost = WALK_OFF_BLOCK_COST + FALL_N_BLOCKS_COST[fallHeight] + frontBreak;
            if (ontoBlock.getBlock() == Blocks.WATER && !BlockStateInterface.isFlowing(ontoBlock)) { // TODO flowing check required here?
                if (Baritone.settings().assumeWalkOnWater.get()) {
                    return IMPOSSIBLE; // TODO fix
                }
                // found a fall into water
                return new MoveResult(destX, newY, destZ, tentativeCost); // TODO incorporate water swim up cost?
            }
            if (ontoBlock.getBlock() == Blocks.FLOWING_WATER) {
                return IMPOSSIBLE;
            }
            if (MovementHelper.canWalkThrough(destX, newY, destZ, ontoBlock)) {
                continue;
            }
            if (!MovementHelper.canWalkOn(destX, newY, destZ, ontoBlock)) {
                return IMPOSSIBLE;
            }
            if (MovementHelper.isBottomSlab(ontoBlock)) {
                return IMPOSSIBLE; // falling onto a half slab is really glitchy, and can cause more fall damage than we'd expect
            }
            if (context.hasWaterBucket() && fallHeight <= context.maxFallHeightBucket() + 1) {
                return new MoveResult(destX, newY + 1, destZ, tentativeCost + context.placeBlockCost()); // this is the block we're falling onto, so dest is +1
            }
            if (fallHeight <= context.maxFallHeightNoWater() + 1) {
                // fallHeight = 4 means onto.up() is 3 blocks down, which is the max
                return new MoveResult(destX, newY + 1, destZ, tentativeCost);
            } else {
                return IMPOSSIBLE;
            }
        }
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        BlockPos playerFeet = playerFeet();
        if (playerFeet.equals(dest)) {
            if (BlockStateInterface.isLiquid(dest) || player().posY - playerFeet.getY() < 0.094) { // lilypads
                // Wait until we're actually on the ground before saying we're done because sometimes we continue to fall if the next action starts immediately
                return state.setStatus(MovementStatus.SUCCESS);
            } else {
                System.out.println(player().posY + " " + playerFeet.getY() + " " + (player().posY - playerFeet.getY()));
            }
        }
        double diffX = player().posX - (dest.getX() + 0.5);
        double diffZ = player().posZ - (dest.getZ() + 0.5);
        double ab = Math.sqrt(diffX * diffX + diffZ * diffZ);
        double x = player().posX - (src.getX() + 0.5);
        double z = player().posZ - (src.getZ() + 0.5);
        double fromStart = Math.sqrt(x * x + z * z);
        if (!playerFeet.equals(dest) || ab > 0.25) {
            BlockPos fakeDest = new BlockPos(dest.getX() * 2 - src.getX(), dest.getY(), dest.getZ() * 2 - src.getZ());
            if (numTicks++ < 20) {
                MovementHelper.moveTowards(state, fakeDest);
                if (fromStart > 1.25) {
                    state.setInput(InputOverrideHandler.Input.MOVE_FORWARD, false);
                    state.setInput(InputOverrideHandler.Input.MOVE_BACK, true);
                }
            } else {
                MovementHelper.moveTowards(state, dest);
            }
        }
        return state;
    }
}
