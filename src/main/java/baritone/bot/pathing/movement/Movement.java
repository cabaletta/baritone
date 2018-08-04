package baritone.bot.pathing.movement;

import baritone.bot.Baritone;
import baritone.bot.pathing.movement.MovementState.MovementStatus;
import baritone.bot.utils.BlockStateInterface;
import baritone.bot.utils.Helper;
import baritone.bot.utils.ToolSet;
import baritone.bot.utils.Utils;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public abstract class Movement implements IMovement, Helper, MovementHelper {

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

    public MovementStatus update() {
//        if(isPrepared(state)) {
//            if (!currentState.isPresent()) {
//                currentState = Optional.of(new MovementState()
//                        .setStatus(MovementStatus.WAITING)
//                        .setGoal());
//            }
//        }
        if(isFinished()) {

        }
        MovementState latestState = updateState(currentState);
        Tuple<Float, Float> rotation = Utils.calcRotationFromVec3d(mc.player.getPositionEyes(1.0F),
                latestState.getGoal().rotation);
        mc.player.setPositionAndRotation(mc.player.posX, mc.player.posY, mc.player.posZ,
                rotation.getFirst(), rotation.getSecond());
        //TODO calculate movement inputs from latestState.getGoal().position
        latestState.inputState.forEach((input, forced) -> {
            Baritone.INSTANCE.getInputOverrideHandler().setInputForceState(input, forced);
        });
        currentState = latestState;

        if (isFinished())
            onFinish();

        return currentState.getStatus();
    }

    private boolean prepare(MovementState state) {
        if(state.getStatus() == MovementStatus.WAITING) {
            return true;
        }
        Optional<BlockPos> cruftPos;
        for(BlockPos blockPos : positionsToBreak) {
            if(MovementHelper.canWalkThrough(blockPos, BlockStateInterface.get(blockPos))) {

            }
        }
        return true;
    }

    public boolean isFinished() {
        return (currentState.getStatus() != MovementStatus.RUNNING
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
        if(!prepare(state))
            return state.setStatus(MovementStatus.PREPPING);
        return state;
    }
}
