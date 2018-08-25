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
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.BlockMagma;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class MovementDiagonal extends Movement {

    private static final double SQRT_2 = Math.sqrt(2);

    public MovementDiagonal(BlockPos start, EnumFacing dir1, EnumFacing dir2) {
        this(start, start.offset(dir1), start.offset(dir2), dir2);
        // super(start, start.offset(dir1).offset(dir2), new BlockPos[]{start.offset(dir1), start.offset(dir1).up(), start.offset(dir2), start.offset(dir2).up(), start.offset(dir1).offset(dir2), start.offset(dir1).offset(dir2).up()}, new BlockPos[]{start.offset(dir1).offset(dir2).down()});
    }

    public MovementDiagonal(BlockPos start, BlockPos dir1, BlockPos dir2, EnumFacing drr2) {
        this(start, dir1.offset(drr2), dir1, dir2);
    }

    public MovementDiagonal(BlockPos start, BlockPos end, BlockPos dir1, BlockPos dir2) {
        super(start, end, new BlockPos[]{dir1, dir1.up(), dir2, dir2.up(), end, end.up()}, new BlockPos[]{end.down()});
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        switch (state.getStatus()) {
            case WAITING:
                state.setStatus(MovementState.MovementStatus.RUNNING);
            case RUNNING:
                break;
            default:
                return state;
        }

        if (playerFeet().equals(dest)) {
            state.setStatus(MovementState.MovementStatus.SUCCESS);
            return state;
        }
        if (!BlockStateInterface.isLiquid(playerFeet()) && Baritone.settings().allowSprint.get()) {
            player().setSprinting(true);
        }
        MovementHelper.moveTowards(state, dest);
        return state;
    }

    @Override
    protected double calculateCost(CalculationContext context) {
        if (!MovementHelper.canWalkThrough(positionsToBreak[4]) || !MovementHelper.canWalkThrough(positionsToBreak[5])) {
            return COST_INF;
        }
        IBlockState destWalkOn = BlockStateInterface.get(positionsToPlace[0]);
        if (!MovementHelper.canWalkOn(positionsToPlace[0], destWalkOn)) {
            return COST_INF;
        }
        double multiplier = WALK_ONE_BLOCK_COST;

        // For either possible soul sand, that affects half of our walking
        if (destWalkOn.getBlock().equals(Blocks.SOUL_SAND)) {
            multiplier += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
        }
        if (BlockStateInterface.get(src.down()).getBlock().equals(Blocks.SOUL_SAND)) {
            multiplier += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
        }

        if (BlockStateInterface.get(positionsToBreak[2].down()).getBlock() instanceof BlockMagma) {
            return COST_INF;
        }
        if (BlockStateInterface.get(positionsToBreak[4].down()).getBlock() instanceof BlockMagma) {
            return COST_INF;
        }
        double optionA = MovementHelper.getMiningDurationTicks(context, positionsToBreak[0]) + MovementHelper.getMiningDurationTicks(context, positionsToBreak[1]);
        double optionB = MovementHelper.getMiningDurationTicks(context, positionsToBreak[2]) + MovementHelper.getMiningDurationTicks(context, positionsToBreak[3]);
        if (optionA != 0 && optionB != 0) {
            return COST_INF;
        }
        if (optionA == 0) {
            if (MovementHelper.avoidWalkingInto(BlockStateInterface.getBlock(positionsToBreak[2]))) {
                return COST_INF;
            }
            if (MovementHelper.avoidWalkingInto(BlockStateInterface.getBlock(positionsToBreak[3]))) {
                return COST_INF;
            }
        }
        if (optionB == 0) {
            if (MovementHelper.avoidWalkingInto(BlockStateInterface.getBlock(positionsToBreak[0]))) {
                return COST_INF;
            }
            if (MovementHelper.avoidWalkingInto(BlockStateInterface.getBlock(positionsToBreak[1]))) {
                return COST_INF;
            }
        }
        if (BlockStateInterface.isWater(src) || BlockStateInterface.isWater(dest)) {
            // Ignore previous multiplier
            // Whatever we were walking on (possibly soul sand) doesn't matter as we're actually floating on water
            // Not even touching the blocks below
            multiplier = WALK_ONE_IN_WATER_COST;
        }
        if (optionA != 0 || optionB != 0) {
            multiplier *= SQRT_2 - 0.001; // TODO tune
        }
        if (multiplier == WALK_ONE_BLOCK_COST && context.canSprint()) {
            // If we aren't edging around anything, and we aren't in water or soul sand
            // We can sprint =D
            multiplier = SPRINT_ONE_BLOCK_COST;
        }
        return multiplier * SQRT_2;
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
        ArrayList<BlockPos> result = new ArrayList<>();
        for (int i = 4; i < 6; i++) {
            if (!MovementHelper.canWalkThrough(positionsToBreak[i])) {
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
            if (!MovementHelper.canWalkThrough(positionsToBreak[i])) {
                result.add(positionsToBreak[i]);
            }
        }
        toWalkIntoCached = result;
        return toWalkIntoCached;
    }
}
