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

    protected Movement(BlockPos src, BlockPos dest) {
        this.src = src;
        this.dest = dest;
    }

    protected Movement(BlockPos src, BlockPos dest, Vec3d rotationTarget) {
        this(src, dest);
        currentState = new MovementState()
                .setLookDirection(rotationTarget)
                .setStatus(MovementStatus.WAITING);
    }

    public abstract double calculateCost(ToolSet ts); // TODO pass in information like whether it's allowed to place throwaway blocks

    @Override
    public void onTick() {
        MovementState latestState = calcState();
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
     * Calculate latest movement state.
     * Gets called once a tick.
     *
     * @return
     */
    public abstract MovementState calcState();
}
