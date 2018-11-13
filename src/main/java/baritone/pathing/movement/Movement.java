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

package baritone.pathing.movement;

import baritone.api.IBaritone;
import baritone.api.pathing.movement.IMovement;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.*;
import baritone.api.utils.input.Input;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.BlockLiquid;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.EmptyChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class Movement implements IMovement, MovementHelper {

    protected static final EnumFacing[] HORIZONTALS = {EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST};

    protected final IBaritone baritone;
    protected final IPlayerContext ctx;

    private MovementState currentState = new MovementState().setStatus(MovementStatus.PREPPING);

    protected final BetterBlockPos src;

    protected final BetterBlockPos dest;

    /**
     * The positions that need to be broken before this movement can ensue
     */
    protected final BetterBlockPos[] positionsToBreak;

    /**
     * The position where we need to place a block before this movement can ensue
     */
    protected final BetterBlockPos positionToPlace;

    private Double cost;

    public List<BlockPos> toBreakCached = null;
    public List<BlockPos> toPlaceCached = null;
    public List<BlockPos> toWalkIntoCached = null;

    private Boolean calculatedWhileLoaded;

    protected Movement(IBaritone baritone, BetterBlockPos src, BetterBlockPos dest, BetterBlockPos[] toBreak, BetterBlockPos toPlace) {
        this.baritone = baritone;
        this.ctx = baritone.getPlayerContext();
        this.src = src;
        this.dest = dest;
        this.positionsToBreak = toBreak;
        this.positionToPlace = toPlace;
    }

    protected Movement(IBaritone baritone, BetterBlockPos src, BetterBlockPos dest, BetterBlockPos[] toBreak) {
        this(baritone, src, dest, toBreak, null);
    }

    @Override
    public double getCost() {
        if (cost == null) {
            cost = calculateCost(new CalculationContext(baritone));
        }
        return cost;
    }

    protected abstract double calculateCost(CalculationContext context);

    @Override
    public double recalculateCost() {
        cost = null;
        return getCost();
    }

    public void override(double cost) {
        this.cost = cost;
    }

    @Override
    public double calculateCostWithoutCaching() {
        return calculateCost(new CalculationContext(baritone));
    }

    /**
     * Handles the execution of the latest Movement
     * State, and offers a Status to the calling class.
     *
     * @return Status
     */
    @Override
    public MovementStatus update() {
        ctx.player().capabilities.isFlying = false;
        currentState = updateState(currentState);
        if (MovementHelper.isLiquid(ctx, ctx.playerFeet())) {
            currentState.setInput(Input.JUMP, true);
        }
        if (ctx.player().isEntityInsideOpaqueBlock()) {
            currentState.setInput(Input.CLICK_LEFT, true);
        }

        // If the movement target has to force the new rotations, or we aren't using silent move, then force the rotations
        currentState.getTarget().getRotation().ifPresent(rotation ->
                baritone.getLookBehavior().updateTarget(
                        rotation,
                        currentState.getTarget().hasToForceRotations()));

        // TODO: calculate movement inputs from latestState.getGoal().position
        // latestState.getTarget().position.ifPresent(null);      NULL CONSUMER REALLY SHOULDN'T BE THE FINAL THING YOU SHOULD REALLY REPLACE THIS WITH ALMOST ACTUALLY ANYTHING ELSE JUST PLEASE DON'T LEAVE IT AS IT IS THANK YOU KANYE

        currentState.getInputStates().forEach((input, forced) -> {
            baritone.getInputOverrideHandler().setInputForceState(input, forced);
        });
        currentState.getInputStates().replaceAll((input, forced) -> false);

        // If the current status indicates a completed movement
        if (currentState.getStatus().isComplete()) {
            baritone.getInputOverrideHandler().clearAllKeys();
        }

        return currentState.getStatus();
    }

    protected boolean prepared(MovementState state) {
        if (state.getStatus() == MovementStatus.WAITING) {
            return true;
        }
        boolean somethingInTheWay = false;
        for (BetterBlockPos blockPos : positionsToBreak) {
            if (!MovementHelper.canWalkThrough(ctx, blockPos) && !(BlockStateInterface.getBlock(ctx, blockPos) instanceof BlockLiquid)) { // can't break liquid, so don't try
                somethingInTheWay = true;
                Optional<Rotation> reachable = RotationUtils.reachable(ctx.player(), blockPos, ctx.playerController().getBlockReachDistance());
                if (reachable.isPresent()) {
                    MovementHelper.switchToBestToolFor(ctx, BlockStateInterface.get(ctx, blockPos));
                    state.setTarget(new MovementState.MovementTarget(reachable.get(), true));
                    if (Objects.equals(RayTraceUtils.getSelectedBlock().orElse(null), blockPos)) {
                        state.setInput(Input.CLICK_LEFT, true);
                    }
                    return false;
                }
                //get rekt minecraft
                //i'm doing it anyway
                //i dont care if theres snow in the way!!!!!!!
                //you dont own me!!!!
                state.setTarget(new MovementState.MovementTarget(RotationUtils.calcRotationFromVec3d(ctx.player().getPositionEyes(1.0F),
                        VecUtils.getBlockPosCenter(blockPos)), true)
                );
                // don't check selectedblock on this one, this is a fallback when we can't see any face directly, it's intended to be breaking the "incorrect" block
                state.setInput(Input.CLICK_LEFT, true);
                return false;
            }
        }
        if (somethingInTheWay) {
            // There's a block or blocks that we can't walk through, but we have no target rotation to reach any
            // So don't return true, actually set state to unreachable
            state.setStatus(MovementStatus.UNREACHABLE);
            return true;
        }
        return true;
    }

    @Override
    public boolean safeToCancel() {
        return safeToCancel(currentState);
    }

    protected boolean safeToCancel(MovementState currentState) {
        return true;
    }

    @Override
    public BetterBlockPos getSrc() {
        return src;
    }

    @Override
    public BetterBlockPos getDest() {
        return dest;
    }

    @Override
    public void reset() {
        currentState = new MovementState().setStatus(MovementStatus.PREPPING);
    }

    /**
     * Calculate latest movement state.
     * Gets called once a tick.
     *
     * @return
     */
    public MovementState updateState(MovementState state) {
        if (!prepared(state)) {
            return state.setStatus(MovementStatus.PREPPING);
        } else if (state.getStatus() == MovementStatus.PREPPING) {
            state.setStatus(MovementStatus.WAITING);
        }

        if (state.getStatus() == MovementStatus.WAITING) {
            state.setStatus(MovementStatus.RUNNING);
        }

        return state;
    }

    @Override
    public BlockPos getDirection() {
        return getDest().subtract(getSrc());
    }

    public void checkLoadedChunk(CalculationContext context) {
        calculatedWhileLoaded = !(context.world().getChunk(getDest()) instanceof EmptyChunk);
    }

    @Override
    public boolean calculatedWhileLoaded() {
        return calculatedWhileLoaded;
    }

    @Override
    public void resetBlockCache() {
        toBreakCached = null;
        toPlaceCached = null;
        toWalkIntoCached = null;
    }

    @Override
    public List<BlockPos> toBreak() {
        if (toBreakCached != null) {
            return toBreakCached;
        }
        List<BlockPos> result = new ArrayList<>();
        for (BetterBlockPos positionToBreak : positionsToBreak) {
            if (!MovementHelper.canWalkThrough(ctx, positionToBreak)) {
                result.add(positionToBreak);
            }
        }
        toBreakCached = result;
        return result;
    }

    @Override
    public List<BlockPos> toPlace() {
        if (toPlaceCached != null) {
            return toPlaceCached;
        }
        List<BlockPos> result = new ArrayList<>();
        if (positionToPlace != null && !MovementHelper.canWalkOn(ctx, positionToPlace)) {
            result.add(positionToPlace);
        }
        toPlaceCached = result;
        return result;
    }

    @Override
    public List<BlockPos> toWalkInto() { // overridden by movementdiagonal
        if (toWalkIntoCached == null) {
            toWalkIntoCached = new ArrayList<>();
        }
        return toWalkIntoCached;
    }
}
