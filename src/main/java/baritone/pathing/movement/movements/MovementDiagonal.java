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
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.BlockStateInterface;
import baritone.utils.pathing.MutableMoveResult;
import com.google.common.collect.ImmutableSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MovementDiagonal extends Movement {

    private static final double SQRT_2 = Math.sqrt(2);

    public MovementDiagonal(IBaritone baritone, BetterBlockPos start, Direction dir1, Direction dir2, int dy) {
        this(baritone, start, start.relative(dir1), start.relative(dir2), dir2, dy);
        // super(start, start.offset(dir1).offset(dir2), new BlockPos[]{start.offset(dir1), start.offset(dir1).up(), start.offset(dir2), start.offset(dir2).up(), start.offset(dir1).offset(dir2), start.offset(dir1).offset(dir2).up()}, new BlockPos[]{start.offset(dir1).offset(dir2).down()});
    }

    private MovementDiagonal(IBaritone baritone, BetterBlockPos start, BetterBlockPos dir1, BetterBlockPos dir2, Direction drr2, int dy) {
        this(baritone, start, dir1.relative(drr2).above(dy), dir1, dir2);
    }

    private MovementDiagonal(IBaritone baritone, BetterBlockPos start, BetterBlockPos end, BetterBlockPos dir1, BetterBlockPos dir2) {
        super(baritone, start, end, new BetterBlockPos[]{dir1, dir1.above(), dir2, dir2.above(), end, end.above()});
    }

    @Override
    protected boolean safeToCancel(MovementState state) {
        //too simple. backfill does not work after cornering with this
        //return context.precomputedData.canWalkOn(ctx, ctx.playerFeet().down());
        LocalPlayer player = ctx.player();
        double offset = 0.25;
        double x = player.position().x;
        double y = player.position().y - 1;
        double z = player.position().z;
        //standard
        if (ctx.playerFeet().equals(src)) {
            return true;
        }
        //both corners are walkable
        if (MovementHelper.canWalkOn(ctx, new BlockPos(src.x, src.y - 1, dest.z))
                && MovementHelper.canWalkOn(ctx, new BlockPos(dest.x, src.y - 1, src.z))) {
            return true;
        }
        //we are in a likely unwalkable corner, check for a supporting block
        if (ctx.playerFeet().equals(new BetterBlockPos(src.x, src.y, dest.z))
                || ctx.playerFeet().equals(new BetterBlockPos(dest.x, src.y, src.z))) {
            return (MovementHelper.canWalkOn(ctx, new BetterBlockPos(x + offset, y, z + offset))
                    || MovementHelper.canWalkOn(ctx, new BetterBlockPos(x + offset, y, z - offset))
                    || MovementHelper.canWalkOn(ctx, new BetterBlockPos(x - offset, y, z + offset))
                    || MovementHelper.canWalkOn(ctx, new BetterBlockPos(x - offset, y, z - offset)));
        }
        return true;
    }

    @Override
    public double calculateCost(CalculationContext context) {
        MutableMoveResult result = new MutableMoveResult();
        cost(context, src.x, src.y, src.z, dest.x, dest.z, result);
        if (result.y != dest.y) {
            return COST_INF; // doesn't apply to us, this position is incorrect
        }
        return result.cost;
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        BetterBlockPos diagA = new BetterBlockPos(src.x, src.y, dest.z);
        BetterBlockPos diagB = new BetterBlockPos(dest.x, src.y, src.z);
        if (dest.y < src.y) {
            return ImmutableSet.of(src, dest.above(), diagA, diagB, dest, diagA.below(), diagB.below());
        }
        if (dest.y > src.y) {
            return ImmutableSet.of(src, src.above(), diagA, diagB, dest, diagA.above(), diagB.above());
        }
        return ImmutableSet.of(src, dest, diagA, diagB);
    }

    public static void cost(CalculationContext context, int x, int y, int z, int destX, int destZ, MutableMoveResult res) {
        if (!MovementHelper.canWalkThrough(context, destX, y + 1, destZ)) {
            return;
        }
        BlockState destInto = context.get(destX, y, destZ);
        BlockState fromDown;
        boolean ascend = false;
        BlockState destWalkOn;
        boolean descend = false;
        boolean frostWalker = false;
        if (!MovementHelper.canWalkThrough(context, destX, y, destZ, destInto)) {
            ascend = true;
            if (!context.allowDiagonalAscend || !MovementHelper.canWalkThrough(context, x, y + 2, z) || !MovementHelper.canWalkOn(context, destX, y, destZ, destInto) || !MovementHelper.canWalkThrough(context, destX, y + 2, destZ)) {
                return;
            }
            destWalkOn = destInto;
            fromDown = context.get(x, y - 1, z);
        } else {
            destWalkOn = context.get(destX, y - 1, destZ);
            fromDown = context.get(x, y - 1, z);
            boolean standingOnABlock = MovementHelper.mustBeSolidToWalkOn(context, x, y - 1, z, fromDown);
            frostWalker = standingOnABlock && MovementHelper.canUseFrostWalker(context, destWalkOn);
            if (!frostWalker && !MovementHelper.canWalkOn(context, destX, y - 1, destZ, destWalkOn)) {
                descend = true;
                if (!context.allowDiagonalDescend || !MovementHelper.canWalkOn(context, destX, y - 2, destZ) || !MovementHelper.canWalkThrough(context, destX, y - 1, destZ, destWalkOn)) {
                    return;
                }
            }
            frostWalker &= !context.assumeWalkOnWater; // do this after checking for descends because jesus can't prevent the water from freezing, it just prevents us from relying on the water freezing
        }
        double multiplier = WALK_ONE_BLOCK_COST;
        // For either possible soul sand, that affects half of our walking
        if (destWalkOn.getBlock() == Blocks.SOUL_SAND) {
            multiplier += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
        } else if (frostWalker) {
            // frostwalker lets us walk on water without the penalty
        } else if (destWalkOn.getBlock() == Blocks.WATER) {
            multiplier += context.walkOnWaterOnePenalty * SQRT_2;
        }
        Block fromDownBlock = fromDown.getBlock();
        if (fromDownBlock == Blocks.LADDER || fromDownBlock == Blocks.VINE) {
            return;
        }
        if (fromDownBlock == Blocks.SOUL_SAND) {
            multiplier += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
        }
        BlockState cuttingOver1 = context.get(x, y - 1, destZ);
        if (cuttingOver1.getBlock() == Blocks.MAGMA_BLOCK || MovementHelper.isLava(cuttingOver1)) {
            return;
        }
        BlockState cuttingOver2 = context.get(destX, y - 1, z);
        if (cuttingOver2.getBlock() == Blocks.MAGMA_BLOCK || MovementHelper.isLava(cuttingOver2)) {
            return;
        }
        boolean water = false;
        BlockState startState = context.get(x, y, z);
        Block startIn = startState.getBlock();
        if (MovementHelper.isWater(startState) || MovementHelper.isWater(destInto)) {
            if (ascend) {
                return;
            }
            // Ignore previous multiplier
            // Whatever we were walking on (possibly soul sand) doesn't matter as we're actually floating on water
            // Not even touching the blocks below
            multiplier = context.waterWalkSpeed;
            water = true;
        }
        BlockState pb0 = context.get(x, y, destZ);
        BlockState pb2 = context.get(destX, y, z);
        if (ascend) {
            boolean ATop = MovementHelper.canWalkThrough(context, x, y + 2, destZ);
            boolean AMid = MovementHelper.canWalkThrough(context, x, y + 1, destZ);
            boolean ALow = MovementHelper.canWalkThrough(context, x, y, destZ, pb0);
            boolean BTop = MovementHelper.canWalkThrough(context, destX, y + 2, z);
            boolean BMid = MovementHelper.canWalkThrough(context, destX, y + 1, z);
            boolean BLow = MovementHelper.canWalkThrough(context, destX, y, z, pb2);
            if ((!(ATop && AMid && ALow) && !(BTop && BMid && BLow)) // no option
                    || MovementHelper.avoidWalkingInto(pb0) // bad
                    || MovementHelper.avoidWalkingInto(pb2) // bad
                    || (ATop && AMid && MovementHelper.canWalkOn(context, x, y, destZ, pb0)) // we could just ascend
                    || (BTop && BMid && MovementHelper.canWalkOn(context, destX, y, z, pb2)) // we could just ascend
                    || (!ATop && AMid && ALow) // head bonk A
                    || (!BTop && BMid && BLow)) { // head bonk B
                return;
            }
            res.cost = multiplier * SQRT_2 + JUMP_ONE_BLOCK_COST;
            res.x = destX;
            res.z = destZ;
            res.y = y + 1;
            return;
        }
        double optionA = MovementHelper.getMiningDurationTicks(context, x, y, destZ, pb0, false);
        double optionB = MovementHelper.getMiningDurationTicks(context, destX, y, z, pb2, false);
        if (optionA != 0 && optionB != 0) {
            // check these one at a time -- if pb0 and pb2 were nonzero, we already know that (optionA != 0 && optionB != 0)
            // so no need to check pb1 as well, might as well return early here
            return;
        }
        BlockState pb1 = context.get(x, y + 1, destZ);
        optionA += MovementHelper.getMiningDurationTicks(context, x, y + 1, destZ, pb1, true);
        if (optionA != 0 && optionB != 0) {
            // same deal, if pb1 makes optionA nonzero and option B already was nonzero, pb3 can't affect the result
            return;
        }
        BlockState pb3 = context.get(destX, y + 1, z);
        if (optionA == 0 && ((MovementHelper.avoidWalkingInto(pb2) && pb2.getBlock() != Blocks.WATER) || MovementHelper.avoidWalkingInto(pb3))) {
            // at this point we're done calculating optionA, so we can check if it's actually possible to edge around in that direction
            return;
        }
        optionB += MovementHelper.getMiningDurationTicks(context, destX, y + 1, z, pb3, true);
        if (optionA != 0 && optionB != 0) {
            // and finally, if the cost is nonzero for both ways to approach this diagonal, it's not possible
            return;
        }
        if (optionB == 0 && ((MovementHelper.avoidWalkingInto(pb0) && pb0.getBlock() != Blocks.WATER) || MovementHelper.avoidWalkingInto(pb1))) {
            // and now that option B is fully calculated, see if we can edge around that way
            return;
        }
        if (optionA != 0 || optionB != 0) {
            multiplier *= SQRT_2 - 0.001; // TODO tune
            if (startIn == Blocks.LADDER || startIn == Blocks.VINE) {
                // edging around doesn't work if doing so would climb a ladder or vine instead of moving sideways
                return;
            }
        } else {
            // only can sprint if not edging around
            if (context.canSprint && !water) {
                // If we aren't edging around anything, and we aren't in water
                // We can sprint =D
                // Don't check for soul sand, since we can sprint on that too
                multiplier *= SPRINT_MULTIPLIER;
            }
        }
        res.cost = multiplier * SQRT_2;
        if (descend) {
            res.cost += Math.max(FALL_N_BLOCKS_COST[1], CENTER_AFTER_FALL_COST);
            res.y = y - 1;
        } else {
            res.y = y;
        }
        res.x = destX;
        res.z = destZ;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        if (ctx.playerFeet().equals(dest)) {
            return state.setStatus(MovementStatus.SUCCESS);
        } else if (!playerInValidPosition() && !(MovementHelper.isLiquid(ctx, src) && getValidPositions().contains(ctx.playerFeet().above()))) {
            return state.setStatus(MovementStatus.UNREACHABLE);
        }
        if (dest.y > src.y && ctx.player().position().y < src.y + 0.1 && ctx.player().horizontalCollision) {
            state.setInput(Input.JUMP, true);
        }
        if (sprint()) {
            state.setInput(Input.SPRINT, true);
        }
        MovementHelper.moveTowards(ctx, state, dest);
        if (ctx.player().isSwimming() && Baritone.settings().swimInWater.value) {
            state.setInput(Input.SPRINT, true);
            state.setTarget(new MovementState.MovementTarget(new Rotation(state.getTarget().getRotation().get().getYaw(), -30), true));
        }
        return state;
    }

    private boolean sprint() {
        if (MovementHelper.isLiquid(ctx, ctx.playerFeet()) && !Baritone.settings().sprintInWater.value) {
            return false;
        }
        for (int i = 0; i < 4; i++) {
            if (!MovementHelper.canWalkThrough(ctx, positionsToBreak[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean prepared(MovementState state) {
        return true;
    }

    @Override
    public List<BlockPos> toBreak(BlockStateInterface bsi) {
        if (toBreakCached != null) {
            return toBreakCached;
        }
        List<BlockPos> result = new ArrayList<>();
        for (int i = 4; i < 6; i++) {
            if (!MovementHelper.canWalkThrough(bsi, positionsToBreak[i].x, positionsToBreak[i].y, positionsToBreak[i].z)) {
                result.add(positionsToBreak[i]);
            }
        }
        toBreakCached = result;
        return result;
    }

    @Override
    public List<BlockPos> toWalkInto(BlockStateInterface bsi) {
        if (toWalkIntoCached == null) {
            toWalkIntoCached = new ArrayList<>();
        }
        List<BlockPos> result = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (!MovementHelper.canWalkThrough(bsi, positionsToBreak[i].x, positionsToBreak[i].y, positionsToBreak[i].z)) {
                result.add(positionsToBreak[i]);
            }
        }
        toWalkIntoCached = result;
        return toWalkIntoCached;
    }
}
