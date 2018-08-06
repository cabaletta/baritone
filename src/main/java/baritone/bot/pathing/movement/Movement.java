package baritone.bot.pathing.movement;

import baritone.bot.Baritone;
import baritone.bot.InputOverrideHandler;
import baritone.bot.behavior.impl.LookBehavior;
import baritone.bot.behavior.impl.LookBehaviorUtils;
import baritone.bot.pathing.movement.MovementState.MovementStatus;
import baritone.bot.utils.BlockStateInterface;
import baritone.bot.utils.Helper;
import baritone.bot.utils.ToolSet;
import baritone.bot.utils.Utils;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

import static baritone.bot.InputOverrideHandler.*;

public abstract class Movement implements Helper, MovementHelper {

    private MovementState currentState = new MovementState().setStatus(MovementStatus.PREPPING);
    protected final BlockPos src;

    protected final BlockPos dest;

    /**
     * The positions that need to be broken before this movement can ensue
     */
    public final BlockPos[] positionsToBreak;

    /**
     * The positions where we need to place a block before this movement can ensue
     */
    public final BlockPos[] positionsToPlace;

    protected Movement(BlockPos src, BlockPos dest, BlockPos[] toBreak, BlockPos[] toPlace) {
        this.src = src;
        this.dest = dest;
        this.positionsToBreak = toBreak;
        this.positionsToPlace = toPlace;
    }

    protected Movement(BlockPos src, BlockPos dest, BlockPos[] toBreak, BlockPos[] toPlace, Vec3d rotationTarget) {
        this(src, dest, toBreak, toPlace);
//        currentState = new MovementState()
//                .setGoal(new )
//                .setStatus(MovementStatus.WAITING);
    }

    public abstract double calculateCost(ToolSet ts); // TODO pass in information like whether it's allowed to place throwaway blocks

    /**
     * Handles the execution of the latest Movement
     * State, and offers a Status to the calling class.
     *
     * @return Status
     */
    public MovementStatus update() {
        MovementState latestState = updateState(currentState);
        latestState.getTarget().rotation.ifPresent(LookBehavior.INSTANCE::updateTarget);
        //TODO calculate movement inputs from latestState.getGoal().position
        latestState.getTarget().position.ifPresent(null); // NULL CONSUMER REALLY SHOULDN'T BE THE FINAL THING YOU SHOULD REALLY REPLACE THIS WITH ALMOST ACTUALLY ANYTHING ELSE JUST PLEASE DON'T LEAVE IT AS IT IS THANK YOU KANYE
        latestState.inputState.forEach((input, forced) -> {
            Baritone.INSTANCE.getInputOverrideHandler().setInputForceState(input, forced);
        });
        currentState = latestState;

        if (isFinished())
            onFinish();

        return currentState.getStatus();
    }

    private boolean prepared(MovementState state) {
        if(state.getStatus() == MovementStatus.WAITING) {
            return true;
        }
        Optional<BlockPos> cruftPos;
        for(BlockPos blockPos : positionsToBreak) {
            if(MovementHelper.canWalkThrough(blockPos, BlockStateInterface.get(blockPos))) {
                Optional<Tuple<Float, Float>> reachable = LookBehaviorUtils.reachable(blockPos);
                reachable.ifPresent(rotation -> {
                    state.setTarget(new MovementState.MovementTarget())
                    state.setInput(Input.CLICK_LEFT, true);
                });
                if (reachable.isPresent())
                    return false;
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
     * Run cleanup on state finish
     */
    public abstract void onFinish();

    /**
     * Calculate latest movement state.
     * Gets called once a tick.
     *
     * @return
     */
    public MovementState updateState(MovementState state) {
        if(!prepared(state))
            return state.setStatus(MovementStatus.PREPPING);
        else if(state.getStatus() == MovementStatus.PREPPING) {
            state.setInput(Input.CLICK_LEFT, false);
        }
        return state;
    }
}
