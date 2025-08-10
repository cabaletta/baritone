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
import baritone.utils.BlockStateInterface;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

public class MovementAscend extends Movement {

    private int ticksWithoutPlacement = 0;
    // Latch to guarantee a jump within a short window once we commit to takeoff
    private int takeoffLatchTicks = 0;

    public MovementAscend(IBaritone baritone, BetterBlockPos src, BetterBlockPos dest) {
        super(baritone, src, dest, new BetterBlockPos[]{dest, src.above(2), dest.above()}, dest.below());
    }

    @Override
    public void reset() {
        super.reset();
        ticksWithoutPlacement = 0;
        takeoffLatchTicks = 0;
    }

    @Override
    public double calculateCost(CalculationContext context) {
        return cost(context, src.x, src.y, src.z, dest.x, dest.z);
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        BetterBlockPos prior = new BetterBlockPos(src.subtract(getDirection()).above()); // sometimes we back up to place the block, also sprint ascends, also skip descend to straight ascend
        return ImmutableSet.of(src,
                src.above(),
                dest,
                prior,
                prior.above()
        );
    }

    public static double cost(CalculationContext context, int x, int y, int z, int destX, int destZ) {
        BlockState toPlace = context.get(destX, y, destZ);
        double additionalPlacementCost = 0;
        if (!MovementHelper.canWalkOn(context, destX, y, destZ, toPlace)) {
            additionalPlacementCost = context.costOfPlacingAt(destX, y, destZ, toPlace);
            if (additionalPlacementCost >= COST_INF) {
                return COST_INF;
            }
            if (!MovementHelper.isReplaceable(destX, y, destZ, toPlace, context.bsi)) {
                return COST_INF;
            }
            boolean foundPlaceOption = false;
            for (int i = 0; i < 5; i++) {
                int againstX = destX + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[i].getStepX();
                int againstY = y + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[i].getStepY();
                int againstZ = destZ + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[i].getStepZ();
                if (againstX == x && againstZ == z) { // we might be able to backplace now, but it doesn't matter because it will have been broken by the time we'd need to use it
                    continue;
                }
                if (MovementHelper.canPlaceAgainst(context.bsi, againstX, againstY, againstZ)) {
                    foundPlaceOption = true;
                    break;
                }
            }
            if (!foundPlaceOption) { // didn't find a valid place =(
                return COST_INF;
            }
        }
        BlockState srcUp2 = context.get(x, y + 2, z); // used lower down anyway
        if (context.get(x, y + 3, z).getBlock() instanceof FallingBlock && (MovementHelper.canWalkThrough(context, x, y + 1, z) || !(srcUp2.getBlock() instanceof FallingBlock))) {//it would fall on us and possibly suffocate us
            // HOWEVER, we assume that we're standing in the start position
            // that means that src and src.up(1) are both air
            // maybe they aren't now, but they will be by the time this starts
            // if the lower one is can't walk through and the upper one is falling, that means that by standing on src
            // (the presupposition of this Movement)
            // we have necessarily already cleared the entire FallingBlock stack
            // on top of our head

            // as in, if we have a block, then two FallingBlocks on top of it
            // and that block is x, y+1, z, and we'd have to clear it to even start this movement
            // we don't need to worry about those FallingBlocks because we've already cleared them
            return COST_INF;
            // you may think we only need to check srcUp2, not srcUp
            // however, in the scenario where glitchy world gen where unsupported sand / gravel generates
            // it's possible srcUp is AIR from the start, and srcUp2 is falling
            // and in that scenario, when we arrive and break srcUp2, that lets srcUp3 fall on us and suffocate us
        }
        BlockState srcDown = context.get(x, y - 1, z);
        if (srcDown.getBlock() == Blocks.LADDER || srcDown.getBlock() == Blocks.VINE) {
            return COST_INF;
        }
        // we can jump from soul sand, but not from a bottom slab
        boolean jumpingFromBottomSlab = MovementHelper.isBottomSlab(srcDown);
        boolean jumpingToBottomSlab = MovementHelper.isBottomSlab(toPlace);
        if (jumpingFromBottomSlab && !jumpingToBottomSlab) {
            return COST_INF;// the only thing we can ascend onto from a bottom slab is another bottom slab
        }
        double walk;
        if (jumpingToBottomSlab) {
            if (jumpingFromBottomSlab) {
                walk = Math.max(JUMP_ONE_BLOCK_COST, WALK_ONE_BLOCK_COST); // we hit space immediately on entering this action
                walk += context.jumpPenalty;
            } else {
                walk = WALK_ONE_BLOCK_COST; // we don't hit space we just walk into the slab
            }
        } else {
            // jumpingFromBottomSlab must be false
            if (toPlace.getBlock() == Blocks.SOUL_SAND) {
                walk = WALK_ONE_OVER_SOUL_SAND_COST;
            } else {
                walk = Math.max(JUMP_ONE_BLOCK_COST, WALK_ONE_BLOCK_COST);
            }
            walk += context.jumpPenalty;
        }

        double totalCost = walk + additionalPlacementCost;
        // start with srcUp2 since we already have its state
        // includeFalling isn't needed because of the falling check above -- if srcUp3 is falling we will have already exited with COST_INF if we'd actually have to break it
        totalCost += MovementHelper.getMiningDurationTicks(context, x, y + 2, z, srcUp2, false);
        if (totalCost >= COST_INF) {
            return COST_INF;
        }
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y + 1, destZ, false);
        if (totalCost >= COST_INF) {
            return COST_INF;
        }
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y + 2, destZ, true);
        return totalCost;
    }

    @Override
    public MovementState updateState(MovementState state) {
        if (ctx.playerFeet().y < src.y) {
            // this check should run even when in preparing state (breaking blocks)
            return state.setStatus(MovementStatus.UNREACHABLE);
        }
        super.updateState(state);
        // Allow approach timing even if state isn't yet RUNNING, as long as the landing block is walkable.
        boolean isRunning = state.getStatus() == MovementStatus.RUNNING;

        if (ctx.playerFeet().equals(dest) || ctx.playerFeet().equals(dest.offset(getDirection().below()))) {
            return state.setStatus(MovementStatus.SUCCESS);
        }

        BlockState jumpingOnto = BlockStateInterface.get(ctx, positionToPlace);
        if (!MovementHelper.canWalkOn(ctx, positionToPlace, jumpingOnto)) {
            ticksWithoutPlacement++;
            if (MovementHelper.attemptToPlaceABlock(state, baritone, dest.below(), false, true) == PlaceResult.READY_TO_PLACE) {
                state.setInput(Input.SNEAK, true);
                if (ctx.player().isCrouching()) {
                    state.setInput(Input.CLICK_RIGHT, true);
                }
            }
            if (ticksWithoutPlacement > 10) {
                // After 10 ticks without placement, we might be standing in the way, move back
                state.setInput(Input.MOVE_BACK, true);
            }

            return state;
        }
        MovementHelper.moveTowards(ctx, state, dest);
        // Maintain forward; adjust sprint and jump timing based on edge distance for clean cadence
        state.setInput(Input.MOVE_FORWARD, true);
        if (Baritone.settings().sprintAscends.value) {
            int dx = Integer.signum(dest.x - src.x);
            int dz = Integer.signum(dest.z - src.z);
            boolean alongX = Math.abs(dx) == 1;

            double px = ctx.player().position().x;
            double pz = ctx.player().position().z;
            double vx = ctx.player().getDeltaMovement().x;
            double vz = ctx.player().getDeltaMovement().z;
            double speedXZ = Math.hypot(vx, vz);
            // Speed factor in [0,1]: 0 near slow walk, 1 at fast sprint/boosts
            double speedFactor = Math.max(0.0, Math.min(1.0, (speedXZ - 0.19D) / 0.08D));
            // Distance to the leading edge of the current block in the direction of travel
            double edgeDist;
            if (alongX) {
                double edgeX = dx > 0 ? (src.x + 1.0D) : (double) src.x;
                edgeDist = Math.max(0.0, dx > 0 ? (edgeX - px) : (px - edgeX));
            } else {
                double edgeZ = dz > 0 ? (src.z + 1.0D) : (double) src.z;
                edgeDist = Math.max(0.0, dz > 0 ? (edgeZ - pz) : (pz - edgeZ));
            }

            // Small lateral tolerance: avoid jumping if we're too far off the center line
            double lateralOffset = alongX ? Math.abs((src.z + 0.5D) - pz) : Math.abs((src.x + 0.5D) - px);
            boolean centered = lateralOffset <= 0.28D;

            boolean onGround = ctx.player().onGround();
            // Secondary proximity using dest center to avoid missing the window if edge calc drifts
            double flatDistToDestCenter = Math.max(Math.abs((dest.x + 0.5D) - px), Math.abs((dest.z + 0.5D) - pz));
            // Dynamic thresholds based on approach speed: earlier taper / earlier jump when too fast
            double taperStart = 0.60D + 0.25D * speedFactor; // up to 0.85 at high speed
            double jumpDist  = 0.32D + 0.10D * speedFactor; // up to 0.42 at high speed
            boolean withinTaper = onGround && centered && edgeDist < taperStart && edgeDist > jumpDist;
            boolean shouldJump = onGround && (
                    (centered && edgeDist <= jumpDist) ||
                    flatDistToDestCenter <= 0.85D ||
                    ctx.player().horizontalCollision
            );

            // Takeoff latch: once we enter taper or are very close, guarantee jump in the next 2 ticks
            if (withinTaper || (onGround && edgeDist <= (jumpDist + 0.08D))) {
                takeoffLatchTicks = Math.max(takeoffLatchTicks, 2 + (int) Math.round(2 * speedFactor));
            }
            if (takeoffLatchTicks > 0) {
                takeoffLatchTicks--;
                // While latched, bias toward jumping sooner rather than later
                if (onGround) {
                    shouldJump = true;
                }
            }

            // Apply inputs without early returns to avoid missing the jump window due to ordering elsewhere
            state.setInput(Input.SPRINT, !withinTaper);
            if (shouldJump) {
                state.setInput(Input.SPRINT, true);
                state.setInput(Input.JUMP, true);
            }
            if (!onGround) {
                state.setInput(Input.SPRINT, true);
            }
        }
        if (MovementHelper.isBottomSlab(jumpingOnto) && !MovementHelper.isBottomSlab(BlockStateInterface.get(ctx, src.below()))) {
            return state; // don't jump while walking from a non double slab into a bottom slab
        }

        if (Baritone.settings().assumeStep.value || ctx.playerFeet().equals(src.above())) {
            // no need to hit space if we're already jumping
            return state;
        }

        int xAxis = Math.abs(dest.x - src.x); // either 0 or 1
        int zAxis = Math.abs(dest.z - src.z); // either 0 or 1
        double flatDistToNext = xAxis * Math.abs((dest.x + 0.5D) - ctx.player().position().x) + zAxis * Math.abs((dest.z + 0.5D) - ctx.player().position().z);
        double sideDist = zAxis * Math.abs((dest.x + 0.5D) - ctx.player().position().x) + xAxis * Math.abs((dest.z + 0.5D) - ctx.player().position().z);

        double lateralMotion = xAxis * ctx.player().getDeltaMovement().z + zAxis * ctx.player().getDeltaMovement().x;
        if (Math.abs(lateralMotion) > 0.1) {
            return state;
        }

        if (headBonkClear()) {
            return state.setInput(Input.JUMP, true);
        }

        if (flatDistToNext > 1.2 || sideDist > 0.2) {
            return state;
        }

        // Once we are pointing the right way and moving, start jumping
        // This is slightly more efficient because otherwise we might start jumping before moving, and fall down without moving onto the block we want to jump onto
        // Also wait until we are close enough, because we might jump and hit our head on an adjacent block
        // Re-engage sprint at jump to preserve momentum up the block
        return state.setInput(Input.SPRINT, true).setInput(Input.JUMP, true);
    }

    public boolean headBonkClear() {
        BetterBlockPos startUp = src.above(2);
        for (int i = 0; i < 4; i++) {
            BetterBlockPos check = startUp.relative(Direction.from2DDataValue(i));
            if (!MovementHelper.canWalkThrough(ctx, check)) {
                // We might bonk our head
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        // if we had to place, don't allow pause
        return state.getStatus() != MovementStatus.RUNNING || ticksWithoutPlacement == 0;
    }
}
