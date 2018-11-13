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

import baritone.api.IBaritone;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class MovementDiagonal extends Movement {

    private static final double SQRT_2 = Math.sqrt(2);

    public MovementDiagonal(IBaritone baritone, BetterBlockPos start, EnumFacing dir1, EnumFacing dir2) {
        this(baritone, start, start.offset(dir1), start.offset(dir2), dir2);
        // super(start, start.offset(dir1).offset(dir2), new BlockPos[]{start.offset(dir1), start.offset(dir1).up(), start.offset(dir2), start.offset(dir2).up(), start.offset(dir1).offset(dir2), start.offset(dir1).offset(dir2).up()}, new BlockPos[]{start.offset(dir1).offset(dir2).down()});
    }

    private MovementDiagonal(IBaritone baritone, BetterBlockPos start, BetterBlockPos dir1, BetterBlockPos dir2, EnumFacing drr2) {
        this(baritone, start, dir1.offset(drr2), dir1, dir2);
    }

    private MovementDiagonal(IBaritone baritone, BetterBlockPos start, BetterBlockPos end, BetterBlockPos dir1, BetterBlockPos dir2) {
        super(baritone, start, end, new BetterBlockPos[]{dir1, dir1.up(), dir2, dir2.up(), end, end.up()});
    }

    @Override
    protected double calculateCost(CalculationContext context) {
        return cost(context, src.x, src.y, src.z, dest.x, dest.z);
    }

    public static double cost(CalculationContext context, int x, int y, int z, int destX, int destZ) {
        Block fromDown = context.get(x, y - 1, z).getBlock();
        if (fromDown == Blocks.LADDER || fromDown == Blocks.VINE) {
            return COST_INF;
        }
        IBlockState destInto = context.get(destX, y, destZ);
        if (!MovementHelper.canWalkThrough(context.bsi(), destX, y, destZ, destInto) || !MovementHelper.canWalkThrough(context.bsi(), destX, y + 1, destZ)) {
            return COST_INF;
        }
        IBlockState destWalkOn = context.get(destX, y - 1, destZ);
        if (!MovementHelper.canWalkOn(context.bsi(), destX, y - 1, destZ, destWalkOn)) {
            return COST_INF;
        }
        double multiplier = WALK_ONE_BLOCK_COST;
        // For either possible soul sand, that affects half of our walking
        if (destWalkOn.getBlock() == Blocks.SOUL_SAND) {
            multiplier += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
        }
        if (fromDown == Blocks.SOUL_SAND) {
            multiplier += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
        }
        Block cuttingOver1 = context.get(x, y - 1, destZ).getBlock();
        if (cuttingOver1 == Blocks.MAGMA || MovementHelper.isLava(cuttingOver1)) {
            return COST_INF;
        }
        Block cuttingOver2 = context.get(destX, y - 1, z).getBlock();
        if (cuttingOver2 == Blocks.MAGMA || MovementHelper.isLava(cuttingOver2)) {
            return COST_INF;
        }
        IBlockState pb0 = context.get(x, y, destZ);
        IBlockState pb2 = context.get(destX, y, z);
        double optionA = MovementHelper.getMiningDurationTicks(context, x, y, destZ, pb0, false);
        double optionB = MovementHelper.getMiningDurationTicks(context, destX, y, z, pb2, false);
        if (optionA != 0 && optionB != 0) {
            // check these one at a time -- if pb0 and pb2 were nonzero, we already know that (optionA != 0 && optionB != 0)
            // so no need to check pb1 as well, might as well return early here
            return COST_INF;
        }
        IBlockState pb1 = context.get(x, y + 1, destZ);
        optionA += MovementHelper.getMiningDurationTicks(context, x, y + 1, destZ, pb1, true);
        if (optionA != 0 && optionB != 0) {
            // same deal, if pb1 makes optionA nonzero and option B already was nonzero, pb3 can't affect the result
            return COST_INF;
        }
        IBlockState pb3 = context.get(destX, y + 1, z);
        if (optionA == 0 && ((MovementHelper.avoidWalkingInto(pb2.getBlock()) && pb2.getBlock() != Blocks.WATER) || (MovementHelper.avoidWalkingInto(pb3.getBlock()) && pb3.getBlock() != Blocks.WATER))) {
            // at this point we're done calculating optionA, so we can check if it's actually possible to edge around in that direction
            return COST_INF;
        }
        optionB += MovementHelper.getMiningDurationTicks(context, destX, y + 1, z, pb3, true);
        if (optionA != 0 && optionB != 0) {
            // and finally, if the cost is nonzero for both ways to approach this diagonal, it's not possible
            return COST_INF;
        }
        if (optionB == 0 && ((MovementHelper.avoidWalkingInto(pb0.getBlock()) && pb0.getBlock() != Blocks.WATER) || (MovementHelper.avoidWalkingInto(pb1.getBlock()) && pb1.getBlock() != Blocks.WATER))) {
            // and now that option B is fully calculated, see if we can edge around that way
            return COST_INF;
        }
        boolean water = false;
        if (MovementHelper.isWater(context.getBlock(x, y, z)) || MovementHelper.isWater(destInto.getBlock())) {
            // Ignore previous multiplier
            // Whatever we were walking on (possibly soul sand) doesn't matter as we're actually floating on water
            // Not even touching the blocks below
            multiplier = context.waterWalkSpeed();
            water = true;
        }
        if (optionA != 0 || optionB != 0) {
            multiplier *= SQRT_2 - 0.001; // TODO tune
        }
        if (context.canSprint() && !water) {
            // If we aren't edging around anything, and we aren't in water
            // We can sprint =D
            // Don't check for soul sand, since we can sprint on that too
            multiplier *= SPRINT_MULTIPLIER;
        }
        return multiplier * SQRT_2;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        if (ctx.playerFeet().equals(dest)) {
            state.setStatus(MovementStatus.SUCCESS);
            return state;
        }
        if (!MovementHelper.isLiquid(ctx, ctx.playerFeet())) {
            state.setInput(Input.SPRINT, true);
        }
        MovementHelper.moveTowards(ctx, state, dest);
        return state;
    }

    @Override
    protected boolean prepared(MovementState state) {
        return true;
    }

    @Override
    public List<BlockPos> toBreak() {
        if (toBreakCached != null) {
            return toBreakCached;
        }
        List<BlockPos> result = new ArrayList<>();
        for (int i = 4; i < 6; i++) {
            if (!MovementHelper.canWalkThrough(ctx, positionsToBreak[i])) {
                result.add(positionsToBreak[i]);
            }
        }
        toBreakCached = result;
        return result;
    }

    @Override
    public List<BlockPos> toWalkInto() {
        if (toWalkIntoCached == null) {
            toWalkIntoCached = new ArrayList<>();
        }
        List<BlockPos> result = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (!MovementHelper.canWalkThrough(ctx, positionsToBreak[i])) {
                result.add(positionsToBreak[i]);
            }
        }
        toWalkIntoCached = result;
        return toWalkIntoCached;
    }
}
