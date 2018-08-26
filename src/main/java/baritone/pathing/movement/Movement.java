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

package baritone.pathing.movement;

import baritone.Baritone;
import baritone.behavior.impl.LookBehavior;
import baritone.behavior.impl.LookBehaviorUtils;
import baritone.pathing.movement.MovementState.MovementStatus;
import baritone.pathing.movement.movements.MovementDownward;
import baritone.pathing.movement.movements.MovementPillar;
import baritone.pathing.movement.movements.MovementTraverse;
import baritone.utils.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockVine;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static baritone.utils.InputOverrideHandler.Input;

public abstract class Movement implements Helper, MovementHelper {

    private MovementState currentState = new MovementState().setStatus(MovementStatus.PREPPING);

    protected final BlockPos src;

    protected final BlockPos dest;

    /**
     * The positions that need to be broken before this movement can ensue
     */
    protected final BlockPos[] positionsToBreak;

    /**
     * The positions where we need to place a block before this movement can ensue
     */
    protected final BlockPos[] positionsToPlace;

    private boolean didBreakLastTick;

    private Double cost;

    protected Movement(BlockPos src, BlockPos dest, BlockPos[] toBreak, BlockPos[] toPlace) {
        this.src = src;
        this.dest = dest;
        this.positionsToBreak = toBreak;
        this.positionsToPlace = toPlace;
    }

    protected Movement(BlockPos src, BlockPos dest, BlockPos[] toBreak, BlockPos[] toPlace, Vec3d rotationTarget) {
        this(src, dest, toBreak, toPlace);
    }

    public double getCost(CalculationContext context) {
        if (cost == null) {
            if (context == null)
                context = new CalculationContext();
            cost = calculateCost0(context);
        }
        return cost;
    }

    private double calculateCost0(CalculationContext context) {
        if (!(this instanceof MovementPillar) && !(this instanceof MovementTraverse) && !(this instanceof MovementDownward)) {
            Block fromDown = BlockStateInterface.get(src.down()).getBlock();
            if (fromDown instanceof BlockLadder || fromDown instanceof BlockVine) {
                return COST_INF;
            }
        }
        return calculateCost(context);
    }

    protected abstract double calculateCost(CalculationContext context);

    public double recalculateCost() {
        cost = null;
        return getCost(null);
    }

    /**
     * Handles the execution of the latest Movement
     * State, and offers a Status to the calling class.
     *
     * @return Status
     */
    public MovementStatus update() {
        player().setSprinting(false);
        MovementState latestState = updateState(currentState);
        if (BlockStateInterface.isLiquid(playerFeet())) {
            latestState.setInput(Input.JUMP, true);
        }

        // If the movement target has to force the new rotations, or we aren't using silent move, then force the rotations
        latestState.getTarget().getRotation().ifPresent(rotation ->
                LookBehavior.INSTANCE.updateTarget(
                        rotation,
                        latestState.getTarget().hasToForceRotations()));

        // TODO: calculate movement inputs from latestState.getGoal().position
        // latestState.getTarget().position.ifPresent(null);      NULL CONSUMER REALLY SHOULDN'T BE THE FINAL THING YOU SHOULD REALLY REPLACE THIS WITH ALMOST ACTUALLY ANYTHING ELSE JUST PLEASE DON'T LEAVE IT AS IT IS THANK YOU KANYE

        this.didBreakLastTick = false;

        latestState.getInputStates().forEach((input, forced) -> {
            RayTraceResult trace = mc.objectMouseOver;
            boolean isBlockTrace = trace != null && trace.typeOfHit == RayTraceResult.Type.BLOCK;
            boolean isLeftClick = forced && input == Input.CLICK_LEFT;

            // If we're forcing left click, we're in a gui screen, and we're looking
            // at a block, break the block without a direct game input manipulation.
            if (mc.currentScreen != null && isLeftClick && isBlockTrace) {
                BlockBreakHelper.tryBreakBlock(trace.getBlockPos(), trace.sideHit);
                this.didBreakLastTick = true;
                return;
            }

            Baritone.INSTANCE.getInputOverrideHandler().setInputForceState(input, forced);
        });
        latestState.getInputStates().replaceAll((input, forced) -> false);

        if (!this.didBreakLastTick)
            BlockBreakHelper.stopBreakingBlock();

        currentState = latestState;

        if (isFinished())
            onFinish(latestState);

        return currentState.getStatus();
    }

    protected boolean prepared(MovementState state) {
        if (state.getStatus() == MovementStatus.WAITING) {
            return true;
        }
        boolean somethingInTheWay = false;
        for (BlockPos blockPos : positionsToBreak) {
            if (!MovementHelper.canWalkThrough(blockPos)) {
                somethingInTheWay = true;
                Optional<Rotation> reachable = LookBehaviorUtils.reachable(blockPos);
                if (reachable.isPresent()) {
                    player().inventory.currentItem = new ToolSet().getBestSlot(BlockStateInterface.get(blockPos));
                    state.setTarget(new MovementState.MovementTarget(reachable.get(), true)).setInput(Input.CLICK_LEFT, true);
                    return false;
                }
                //get rekt minecraft
                //i'm doing it anyway
                //i dont care if theres snow in the way!!!!!!!
                //you dont own me!!!!
                state.setTarget(new MovementState.MovementTarget(Utils.calcRotationFromVec3d(mc.player.getPositionEyes(1.0F),
                        Utils.getBlockPosCenter(blockPos)), true)
                ).setInput(InputOverrideHandler.Input.CLICK_LEFT, true);
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

    public boolean isFinished() {
        return (currentState.getStatus() != MovementStatus.RUNNING
                && currentState.getStatus() != MovementStatus.PREPPING
                && currentState.getStatus() != MovementStatus.WAITING);
    }

    public BlockPos getSrc() {
        return src;
    }

    public BlockPos getDest() {
        return dest;
    }

    /**
     * Run cleanup on state finish and declare success.
     */
    public void onFinish(MovementState state) {
        state.getInputStates().replaceAll((input, forced) -> false);
        state.getInputStates().forEach((input, forced) -> Baritone.INSTANCE.getInputOverrideHandler().setInputForceState(input, forced));
    }

    public void cancel() {
        currentState.getInputStates().replaceAll((input, forced) -> false);
        currentState.getInputStates().forEach((input, forced) -> Baritone.INSTANCE.getInputOverrideHandler().setInputForceState(input, forced));
        currentState.setStatus(MovementStatus.CANCELED);
    }

    public void reset() {
        currentState = new MovementState().setStatus(MovementStatus.PREPPING);
    }

    public double getTotalHardnessOfBlocksToBreak(CalculationContext ctx) {
        /*
        double sum = 0;
        HashSet<BlockPos> toBreak = new HashSet();
        for (BlockPos positionsToBreak1 : positionsToBreak) {
            toBreak.add(positionsToBreak1);
            if (this instanceof ActionFall) {//if we are digging straight down, assume we have already broken the sand above us
                continue;
            }
            BlockPos tmp = positionsToBreak1.up();
            while (canFall(tmp)) {
                toBreak.add(tmp);
                tmp = tmp.up();
            }
        }
        for (BlockPos pos : toBreak) {
            sum += getHardness(ts, Baritone.get(pos), pos);
            if (sum >= COST_INF) {
                return COST_INF;
            }
        }
        if (!Baritone.allowBreakOrPlace || !Baritone.hasThrowaway) {
            for (int i = 0; i < blocksToPlace.length; i++) {
                if (!canWalkOn(positionsToPlace[i])) {
                    return COST_INF;
                }
            }
        }*/
        //^ the above implementation properly deals with falling blocks, TODO integrate
        double sum = 0;
        for (BlockPos pos : positionsToBreak) {
            sum += MovementHelper.getMiningDurationTicks(ctx, pos);
            if (sum >= COST_INF) {
                return COST_INF;
            }
        }
        return sum;
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

    public List<BlockPos> toBreakCached = null;
    public List<BlockPos> toPlaceCached = null;
    public List<BlockPos> toWalkIntoCached = null;

    public List<BlockPos> toBreak() {
        if (toBreakCached != null) {
            return toBreakCached;
        }
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos positionToBreak : positionsToBreak) {
            if (!MovementHelper.canWalkThrough(positionToBreak)) {
                result.add(positionToBreak);
            }
        }
        toBreakCached = result;
        return result;
    }

    public List<BlockPos> toPlace() {
        if (toPlaceCached != null) {
            return toPlaceCached;
        }
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos positionToBreak : positionsToPlace) {
            if (!MovementHelper.canWalkOn(positionToBreak)) {
                result.add(positionToBreak);
            }
        }
        toPlaceCached = result;
        return result;
    }

    public List<BlockPos> toWalkInto() { // overridden by movementdiagonal
        if (toWalkIntoCached == null) {
            toWalkIntoCached = new ArrayList<>();
        }
        return toWalkIntoCached;
    }
}
