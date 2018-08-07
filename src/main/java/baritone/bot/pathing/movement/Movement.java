package baritone.bot.pathing.movement;

import baritone.bot.Baritone;
import baritone.bot.InputOverrideHandler;
import baritone.bot.behavior.impl.LookBehavior;
import baritone.bot.behavior.impl.LookBehaviorUtils;
import baritone.bot.pathing.movement.MovementState.MovementStatus;
import baritone.bot.utils.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Optional;

import static baritone.bot.InputOverrideHandler.Input;

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

    public double getCost(ToolSet ts) {
        if (cost == null) {
            if (ts == null) {
                ts = new ToolSet();
            }
            cost = calculateCost(ts);
        }
        return cost;
    }

    protected abstract double calculateCost(ToolSet ts); // TODO pass in information like whether it's allowed to place throwaway blocks

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
        MovementState latestState = updateState(currentState);
        if (BlockStateInterface.isLiquid(playerFeet())) {
            latestState.setInput(Input.JUMP, true);
        }
        latestState.getTarget().getRotation().ifPresent(LookBehavior.INSTANCE::updateTarget);
        // TODO: calculate movement inputs from latestState.getGoal().position
        // latestState.getTarget().position.ifPresent(null);      NULL CONSUMER REALLY SHOULDN'T BE THE FINAL THING YOU SHOULD REALLY REPLACE THIS WITH ALMOST ACTUALLY ANYTHING ELSE JUST PLEASE DON'T LEAVE IT AS IT IS THANK YOU KANYE
        latestState.getInputStates().forEach((input, forced) -> {
            Baritone.INSTANCE.getInputOverrideHandler().setInputForceState(input, forced);
            System.out.println(input + " AND " + forced);
        });
        latestState.getInputStates().replaceAll((input, forced) -> false);
        currentState = latestState;

        if (isFinished())
            onFinish(latestState);

        return currentState.getStatus();
    }

    private boolean prepared(MovementState state) {
        if (state.getStatus() == MovementStatus.WAITING)
            return true;

        for (BlockPos blockPos : positionsToBreak) {
            if (!MovementHelper.canWalkThrough(blockPos)) {
                Optional<Rotation> reachable = LookBehaviorUtils.reachable(blockPos);
                if (reachable.isPresent()) {
                    state.setTarget(new MovementState.MovementTarget(reachable.get())).setInput(Input.CLICK_LEFT, true);
                    return false;
                }
            }
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
        state.setStatus(MovementStatus.SUCCESS);
    }

    public void cancel() {
        currentState.getInputStates().replaceAll((input, forced) -> false);
        currentState.getInputStates().forEach((input, forced) -> Baritone.INSTANCE.getInputOverrideHandler().setInputForceState(input, forced));
        currentState.setStatus(MovementStatus.CANCELED);
    }


    public double getTotalHardnessOfBlocksToBreak(ToolSet ts) {
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
            sum += MovementHelper.getMiningDurationTicks(ts, pos);
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
        if (!prepared(state))
            return state.setStatus(MovementStatus.PREPPING);
        else if (state.getStatus() == MovementStatus.PREPPING) {
            state.setStatus(MovementStatus.WAITING);
        }
        return state;
    }

    public ArrayList<BlockPos> toBreakCached = null;
    public ArrayList<BlockPos> toPlaceCached = null;

    public ArrayList<BlockPos> toBreak() {
        if (toBreakCached != null) {
            return toBreakCached;
        }
        ArrayList<BlockPos> result = new ArrayList<>();
        for (BlockPos positionToBreak : positionsToBreak) {
            if (!MovementHelper.canWalkThrough(positionToBreak)) {
                result.add(positionToBreak);
            }
        }
        toBreakCached = result;
        return result;
    }

    public ArrayList<BlockPos> toPlace() {
        if (toPlaceCached != null) {
            return toPlaceCached;
        }
        ArrayList<BlockPos> result = new ArrayList<>();
        for (BlockPos positionToBreak : positionsToPlace) {
            if (!MovementHelper.canWalkOn(positionToBreak)) {
                result.add(positionToBreak);
            }
        }
        toPlaceCached = result;
        return result;
    }

    protected void moveTowards(BlockPos pos) {
        currentState.setTarget(new MovementState.MovementTarget(new Rotation(Utils.calcRotationFromVec3d(playerHead(), new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), playerRotations()).getFirst(), player().rotationPitch)))
                .setInput(InputOverrideHandler.Input.MOVE_FORWARD, true);
    }
}
