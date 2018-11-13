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
import baritone.api.IBaritone;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.pathing.MutableMoveResult;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

public class MovementDescend extends Movement {

    private int numTicks = 0;

    public MovementDescend(IBaritone baritone, BetterBlockPos start, BetterBlockPos end) {
        super(baritone, start, end, new BetterBlockPos[]{end.up(2), end.up(), end}, end.down());
    }

    @Override
    public void reset() {
        super.reset();
        numTicks = 0;
    }

    @Override
    protected double calculateCost(CalculationContext context) {
        MutableMoveResult result = new MutableMoveResult();
        cost(context, src.x, src.y, src.z, dest.x, dest.z, result);
        if (result.y != dest.y) {
            return COST_INF; // doesn't apply to us, this position is a fall not a descend
        }
        return result.cost;
    }

    public static void cost(CalculationContext context, int x, int y, int z, int destX, int destZ, MutableMoveResult res) {
        Block fromDown = context.get(x, y - 1, z).getBlock();
        if (fromDown == Blocks.LADDER || fromDown == Blocks.VINE) {
            return;
        }

        double totalCost = 0;
        IBlockState destDown = context.get(destX, y - 1, destZ);
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y - 1, destZ, destDown, false);
        if (totalCost >= COST_INF) {
            return;
        }
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y, destZ, false);
        if (totalCost >= COST_INF) {
            return;
        }
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y + 1, destZ, true); // only the top block in the 3 we need to mine needs to consider the falling blocks above
        if (totalCost >= COST_INF) {
            return;
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

        IBlockState below = context.get(destX, y - 2, destZ);
        if (!MovementHelper.canWalkOn(context, destX, y - 2, destZ, below)) {
            dynamicFallCost(context, x, y, z, destX, destZ, totalCost, below, res);
            return;
        }

        if (destDown.getBlock() == Blocks.LADDER || destDown.getBlock() == Blocks.VINE) {
            return;
        }

        // we walk half the block plus 0.3 to get to the edge, then we walk the other 0.2 while simultaneously falling (math.max because of how it's in parallel)
        double walk = WALK_OFF_BLOCK_COST;
        if (fromDown == Blocks.SOUL_SAND) {
            // use this ratio to apply the soul sand speed penalty to our 0.8 block distance
            walk = WALK_ONE_OVER_SOUL_SAND_COST;
        }
        totalCost += walk + Math.max(FALL_N_BLOCKS_COST[1], CENTER_AFTER_FALL_COST);
        res.x = destX;
        res.y = y - 1;
        res.z = destZ;
        res.cost = totalCost;
    }

    public static void dynamicFallCost(CalculationContext context, int x, int y, int z, int destX, int destZ, double frontBreak, IBlockState below, MutableMoveResult res) {
        if (frontBreak != 0 && context.get(destX, y + 2, destZ).getBlock() instanceof BlockFalling) {
            // if frontBreak is 0 we can actually get through this without updating the falling block and making it actually fall
            // but if frontBreak is nonzero, we're breaking blocks in front, so don't let anything fall through this column,
            // and potentially replace the water we're going to fall into
            return;
        }
        if (!MovementHelper.canWalkThrough(context, destX, y - 2, destZ, below) && below.getBlock() != Blocks.WATER) {
            return;
        }
        for (int fallHeight = 3; true; fallHeight++) {
            int newY = y - fallHeight;
            if (newY < 0) {
                // when pathing in the end, where you could plausibly fall into the void
                // this check prevents it from getting the block at y=-1 and crashing
                return;
            }
            IBlockState ontoBlock = context.get(destX, newY, destZ);
            double tentativeCost = WALK_OFF_BLOCK_COST + FALL_N_BLOCKS_COST[fallHeight] + frontBreak;
            if (ontoBlock.getBlock() == Blocks.WATER && !MovementHelper.isFlowing(ontoBlock) && context.getBlock(destX, newY + 1, destZ) != Blocks.WATERLILY) { // TODO flowing check required here?
                // lilypads are canWalkThrough, but we can't end a fall that should be broken by water if it's covered by a lilypad
                // however, don't return impossible in the lilypad scenario, because we could still jump right on it (water that's below a lilypad is canWalkOn so it works)
                if (Baritone.settings().assumeWalkOnWater.get()) {
                    return; // TODO fix
                }
                // found a fall into water
                res.x = destX;
                res.y = newY;
                res.z = destZ;
                res.cost = tentativeCost;// TODO incorporate water swim up cost?
                return;
            }
            if (ontoBlock.getBlock() == Blocks.FLOWING_WATER) {
                return;
            }
            if (MovementHelper.canWalkThrough(context, destX, newY, destZ, ontoBlock)) {
                continue;
            }
            if (!MovementHelper.canWalkOn(context, destX, newY, destZ, ontoBlock)) {
                return;
            }
            if (MovementHelper.isBottomSlab(ontoBlock)) {
                return; // falling onto a half slab is really glitchy, and can cause more fall damage than we'd expect
            }
            if (context.hasWaterBucket() && fallHeight <= context.maxFallHeightBucket() + 1) {
                res.x = destX;
                res.y = newY + 1;// this is the block we're falling onto, so dest is +1
                res.z = destZ;
                res.cost = tentativeCost + context.placeBlockCost();
                return;
            }
            if (fallHeight <= context.maxFallHeightNoWater() + 1) {
                // fallHeight = 4 means onto.up() is 3 blocks down, which is the max
                res.x = destX;
                res.y = newY + 1;
                res.z = destZ;
                res.cost = tentativeCost;
                return;
            } else {
                return;
            }
        }
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        BlockPos playerFeet = ctx.playerFeet();
        if (playerFeet.equals(dest) && (MovementHelper.isLiquid(dest) || ctx.player().posY - playerFeet.getY() < 0.094)) { // lilypads
            // Wait until we're actually on the ground before saying we're done because sometimes we continue to fall if the next action starts immediately
            return state.setStatus(MovementStatus.SUCCESS);
            /* else {
                // System.out.println(player().posY + " " + playerFeet.getY() + " " + (player().posY - playerFeet.getY()));
            }*/
        }
        double diffX = ctx.player().posX - (dest.getX() + 0.5);
        double diffZ = ctx.player().posZ - (dest.getZ() + 0.5);
        double ab = Math.sqrt(diffX * diffX + diffZ * diffZ);
        double x = ctx.player().posX - (src.getX() + 0.5);
        double z = ctx.player().posZ - (src.getZ() + 0.5);
        double fromStart = Math.sqrt(x * x + z * z);
        if (!playerFeet.equals(dest) || ab > 0.25) {
            BlockPos fakeDest = new BlockPos(dest.getX() * 2 - src.getX(), dest.getY(), dest.getZ() * 2 - src.getZ());
            if (numTicks++ < 20) {
                MovementHelper.moveTowards(ctx, state, fakeDest);
                if (fromStart > 1.25) {
                    state.setInput(Input.MOVE_FORWARD, false);
                    state.setInput(Input.MOVE_BACK, true);
                }
            } else {
                MovementHelper.moveTowards(ctx, state, dest);
            }
        }
        return state;
    }
}
