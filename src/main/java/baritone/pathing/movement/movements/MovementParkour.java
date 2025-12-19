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
import baritone.api.utils.Pair;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.*;
import baritone.utils.BlockStateInterface;
import baritone.utils.pathing.MutableMoveResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.WaterFluid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MovementParkour extends Movement {

    private static final BetterBlockPos[] EMPTY = new BetterBlockPos[]{};
    private final BetterBlockPos dest;

    public MovementParkour(IBaritone baritone, BetterBlockPos src, BetterBlockPos dest) {
        super(baritone, src, dest, EMPTY, new BetterBlockPos[] {dest});
        this.dest = dest;
    }

    public static List<Pair<Offset, Double>> cost(CalculationContext context, int x, int y, int z) {
        List<Pair<Offset, Double>> costs = new ArrayList<>();
        if (!context.allowParkour) {
            return costs;
        }
        if (!context.allowJumpAtBuildLimit && y >= context.world.getMaxBuildHeight()) {
            return costs;
        }
        if (!MovementHelper.fullyPassable(context, x, y + 2, z)) {
            return costs;
        }
        BlockState standingOn = context.get(x, y - 1, z);
        if (standingOn.getBlock() == Blocks.VINE || standingOn.getBlock() == Blocks.LADDER || standingOn.getBlock() instanceof StairBlock || MovementHelper.isBottomSlab(standingOn)) {
            return costs;
        }
        // we can't jump from (frozen) water with assumeWalkOnWater because we can't be sure it will be frozen
        if (context.assumeWalkOnWater && !standingOn.getFluidState().isEmpty()) {
            return costs;
        }
        if (!context.get(x, y, z).getFluidState().isEmpty()) {
            return costs; // can't jump out of water
        }
        int maxJump = getMaxJump(context, x, y, z, standingOn);

        // check parkour jumps from largest to smallest for obstacles/walls and landing positions
        for (int xJump = -maxJump; xJump <= maxJump; xJump++) {
            for (int zJump = -maxJump; zJump <= maxJump; zJump++) {
                if (xJump != 0 || zJump != 0) {
                    double cost = cost(context, x, y, z, x + xJump, y, z + zJump);
                    if (cost < COST_INF) {
                        costs.add(new Pair<>(new Offset(xJump, 0, zJump), cost));
                    }
                    // TODO ascending
                }
            }
        }
        return costs;

        // parkour place starts here
//        if (!context.allowParkourPlace) {
//            return ret;
//        }
//        // check parkour jumps from largest to smallest for positions to place blocks
//        for (int i = verifiedMaxJump; i >= 2; i--) {
//            int destX = x + i * xDiff;
//            int destZ = z + i * zDiff;
//            BlockState toReplace = context.get(destX, y - 1, destZ);
//            double placeCost = context.costOfPlacingAt(destX, y - 1, destZ, toReplace);
//            if (placeCost >= COST_INF) {
//                continue;
//            }
//            if (!MovementHelper.isReplaceable(destX, y - 1, destZ, toReplace, context.bsi)) {
//                continue;
//            }
//            if (!checkOvershootSafety(context.bsi, destX + xDiff, y, destZ + zDiff)) {
//                continue;
//            }
//            for (Direction direction : HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP) {
//                int againstX = destX + direction.getStepX();
//                int againstY = y - 1 + direction.getStepY();
//                int againstZ = destZ + direction.getStepZ();
//                if (againstX == destX - xDiff && againstZ == destZ - zDiff) { // we can't turn around that fast
//                    continue;
//                }
//                if (MovementHelper.canPlaceAgainst(context.bsi, againstX, againstY, againstZ)) {
//                    res.x = destX;
//                    res.y = y;
//                    res.z = destZ;
//                    res.cost = costFromJumpDistance(i) + placeCost + context.jumpPenalty;
//                    return;
//                }
//            }
//        }
    }

    public static double cost(CalculationContext context, int x, int y, int z, int destX, int destY, int destZ) {
        int maxJump = getMaxJump(context, x, y, z, context.get(x, y - 1, z));
        if (destX - x > maxJump || destZ - z > maxJump) {
            return COST_INF;
        }
        if (!MovementHelper.fullyPassable(context, destX, y, destZ)) { // TODO needed check?
            // most common case at the top -- the adjacent block isn't air
            return COST_INF;
        }
        BlockState adj = context.get(destX, y - 1, destZ);
//        if (MovementHelper.canWalkOn(context, x + xDiff, y - 1, z + zDiff, adj)) { // don't parkour if we could just traverse (for now)
//            // second most common case -- we could just traverse not parkour
//            return;
//        }
        if (MovementHelper.avoidWalkingInto(adj) && !(adj.getFluidState().getType() instanceof WaterFluid)) { // magma sucks
            return COST_INF;
        }
        if (!MovementHelper.fullyPassable(context, destX, y + 1, destZ)) {
            return COST_INF;
        }
        if (!MovementHelper.fullyPassable(context, destX, y + 2, destZ)) {
            return COST_INF;
        }

        // check head/feet
        if (!MovementHelper.fullyPassable(context, destX, y + 1, destZ)) {
            return COST_INF;
        }
        if (!MovementHelper.fullyPassable(context, destX, y + 2, destZ)) {
            return COST_INF;
        }

        BlockState destInto = context.bsi.get0(destX, y, destZ);
        if (!MovementHelper.fullyPassable(context, destX, y, destZ, destInto)) {
            return COST_INF;
        }

        BlockState landingOn = context.bsi.get0(destX, y - 1, destZ);
        if (!MovementHelper.canWalkOn(context, destX, y - 1, destZ, landingOn)) {
            return COST_INF;
        }

//        // check for ascend landing position
//        BlockState destInto = context.bsi.get0(destX, y, destZ);
//        if (!MovementHelper.fullyPassable(context, destX, y, destZ, destInto)) {
//            if (maxJump <= 3 && context.allowParkourAscend && context.canSprint && MovementHelper.canWalkOn(context, destX, y, destZ, destInto) && checkOvershootSafety(context.bsi, destX + xDiff, y + 1, destZ + zDiff)) {
//                res.x = destX;
//                res.y = y + 1;
//                res.z = destZ;
//                res.cost = i * SPRINT_ONE_BLOCK_COST + context.jumpPenalty;
//                return;
//            } else {
//                return COST_INF;
//            }
//        }
        // check for flat landing position
//        BlockState landingOn = context.bsi.get0(destX, y - 1, destZ);
//        // farmland needs to be canWalkOn otherwise farm can never work at all, but we want to specifically disallow ending a jump on farmland haha
//        // frostwalker works here because we can't jump from possibly unfrozen water
//        if ((landingOn.getBlock() != Blocks.FARMLAND && MovementHelper.canWalkOn(context, destX, y - 1, destZ, landingOn))
//                || (Math.min(16, context.frostWalker + 2) >= i && MovementHelper.canUseFrostWalker(context, landingOn))
//        ) {
//            if (checkOvershootSafety(context.bsi, destX, destY, destZ)) {
//                return costFromJumpDistance((int)Mth.sqrt((x - destX) * (x - destX) + (z - destZ) * (z - destZ))) + context.jumpPenalty; // TODO cost calculation changes for diag jumps
//            }
//            return COST_INF;
//        }
        return costFromJumpDistance(maxJump);
    }

    private static int getMaxJump(CalculationContext context, int x, int y, int z, BlockState standingOn) {
        if (context.allowWalkOnMagmaBlocks && standingOn.is(Blocks.MAGMA_BLOCK)) {
            return 2;
        } else if (standingOn.getBlock() == Blocks.SOUL_SAND) {
            return 2; // 1 block gap
        } else if (context.canSprint) {
            return 4;
        } else {
            return 3;
        }
    }

    private static boolean checkOvershootSafety(BlockStateInterface bsi, int x, int y, int z) {
        // we're going to walk into these two blocks after the landing of the parkour anyway, so make sure they aren't avoidWalkingInto
        return !MovementHelper.avoidWalkingInto(bsi.get0(x, y, z)) && !MovementHelper.avoidWalkingInto(bsi.get0(x, y + 1, z));
    }

    private static double costFromJumpDistance(int dist) {
        // TODO calculate jump costs
        return switch (dist) {
            case 2 -> WALK_ONE_BLOCK_COST * 2 / 2; // IDK LOL
            case 3 -> WALK_ONE_BLOCK_COST * 3 / 2;
            case 4 -> SPRINT_ONE_BLOCK_COST * 4 / 2;
            default -> throw new IllegalStateException("LOL " + dist);
        };
    }


    @Override
    public double calculateCost(CalculationContext context) {
        return cost(context, src.x, src.y, src.z, dest.x, dest.y, dest.z);
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        Set<BetterBlockPos> set = new HashSet<>();
        int xDist = Math.abs(src.x - dest.x);
        int zDist = Math.abs(src.z - dest.z);
        for (int x = -xDist; x <= xDist; x++) {
            for (int z = -zDist; z <= zDist; z++) {
                if ((src.x != x || src.z != z) && x * x + z * z < 20) {
                    set.add(new BetterBlockPos(src.x + x, src.y, src.z + z));
                }
            }
        }
        return set;
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        // once this movement is instantiated, the state is default to PREPPING
        // but once it's ticked for the first time it changes to RUNNING
        // since we don't really know anything about momentum, it suffices to say Parkour can only be canceled on the 0th tick
        return state.getStatus() != MovementStatus.RUNNING;
    }

    @Override
    public MovementState updateState(MovementState state) {
        System.out.println("sdlpihsdf");
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }
        if (ctx.playerFeet().y < src.y) {
            // we have fallen
            logDebug("sorry");
            return state.setStatus(MovementStatus.UNREACHABLE);
        }
//        if (dist >= 4 || ascend) {
//            state.setInput(Input.SPRINT, true);
//        }
        state.setInput(Input.SPRINT, true);
        if (Baritone.settings().allowWalkOnMagmaBlocks.value && ctx.world().getBlockState(ctx.playerFeet().below()).is(Blocks.MAGMA_BLOCK)) {
            state.setInput(Input.SNEAK, true);
        }

        if (Math.abs(ctx.playerFeetAsVec().x() - dest.getCenter().x()) > ctx.playerMotion().x() &&
                Math.abs(ctx.playerFeetAsVec().z() - dest.getCenter().z()) > ctx.playerMotion().z()) {
            MovementHelper.moveTowards(ctx, state, dest);
        }
        if (ctx.playerFeet().equals(dest)) {
            if (ctx.player().isOnGround()) {
                state.setStatus(MovementStatus.SUCCESS);
            }
        } else if (ctx.playerFeet().distanceSq(dest) > 2) {
            state.setInput(Input.JUMP, true);
        }
        MovementHelper.moveTowards(ctx, state, dest);
//        } else if (!ctx.playerFeet().equals(src)) {
//            if (ctx.playerFeet().equals(src.relative(direction)) || ctx.player().position().y - src.y > 0.0001) {
//                if (Baritone.settings().allowPlace.value // see PR #3775
//                        && ((Baritone) baritone).getInventoryBehavior().hasGenericThrowaway()
//                        && !MovementHelper.canWalkOn(ctx, dest.below())
//                        && !ctx.player().isOnGround()
//                        && MovementHelper.attemptToPlaceABlock(state, baritone, dest.below(), true, false) == PlaceResult.READY_TO_PLACE
//                ) {
//                    // go in the opposite order to check DOWN before all horizontals -- down is preferable because you don't have to look to the side while in midair, which could mess up the trajectory
//                    state.setInput(Input.CLICK_RIGHT, true);
//                }
//                // prevent jumping too late by checking for ascend
//                if (dist == 3 && !ascend) { // this is a 2 block gap, dest = src + direction * 3
//                    double xDiff = (src.x + 0.5) - ctx.player().position().x;
//                    double zDiff = (src.z + 0.5) - ctx.player().position().z;
//                    double distFromStart = Math.max(Math.abs(xDiff), Math.abs(zDiff));
//                    if (distFromStart < 0.7) {
//                        return state;
//                    }
//                }
//
//                state.setInput(Input.JUMP, true);
//            } else if (!ctx.playerFeet().equals(dest.relative(direction, -1))) {
//                state.setInput(Input.SPRINT, false);
//                if (ctx.playerFeet().equals(src.relative(direction, -1))) {
//                    MovementHelper.moveTowards(ctx, state, src);
//                } else {
//                    MovementHelper.moveTowards(ctx, state, src.relative(direction, -1));
//                }
//            }
//        }
        return state;
    }
}
