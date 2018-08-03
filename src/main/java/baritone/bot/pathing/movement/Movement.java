package baritone.bot.pathing.movement;

import baritone.bot.Baritone;
import baritone.bot.event.AbstractGameEventListener;
import baritone.bot.pathing.movement.MovementState.MovementStatus;
import baritone.bot.utils.Helper;
import baritone.bot.utils.ToolSet;
import baritone.bot.utils.Utils;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public abstract class Movement implements AbstractGameEventListener, Helper, MovementHelper {

    protected MovementState currentState;

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
        currentState = new MovementState()
                .setLookDirection(rotationTarget)
                .setStatus(MovementStatus.WAITING);
    }

    public abstract double calculateCost(ToolSet ts); // TODO pass in information like whether it's allowed to place throwaway blocks

    @Override
    public void onTick() {
        MovementState latestState = updateState();
        Optional<Vec3d> orientation = latestState.getGoal().rotation;
        if (orientation.isPresent()) {
            Tuple<Float, Float> rotation = Utils.calcRotationFromVec3d(mc.player.getPositionEyes(1.0F),
                    orientation.get());
            mc.player.setPositionAndRotation(mc.player.posX, mc.player.posY, mc.player.posZ,
                    rotation.getFirst(), rotation.getSecond());
        }
        //TODO calculate movement inputs from latestState.getGoal().position
        latestState.inputState.forEach((input, forced) -> {
            Baritone.INSTANCE.getInputOverrideHandler().setInputForceState(input, forced);
        });
        currentState = latestState;

        if (isFinished())
            onFinish();
            return;
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
    public abstract MovementState updateState();
}
