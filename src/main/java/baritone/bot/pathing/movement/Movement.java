package baritone.bot.pathing.movement;

import baritone.bot.Baritone;
import baritone.bot.event.AbstractGameEventListener;
import baritone.bot.pathing.movement.MovementState.MovementStatus;
import baritone.bot.utils.Helper;
import baritone.bot.utils.Utils;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public abstract class Movement implements AbstractGameEventListener, Helper, MovementHelper {

    protected MovementState currentState;
    protected final BlockPos src;
    protected final BlockPos dest;

    protected Movement(BlockPos src, BlockPos dest) {
        this(src, dest, Utils.calcCenterFromCoords(dest, mc.world));
    }


    protected Movement(BlockPos src, BlockPos dest, Vec3d rotationTarget) {
        this.src = src;
        this.dest = dest;
        currentState = new MovementState()
                .setGoal(new MovementState.MovementGoal(dest, rotationTarget))
                .setStatus(MovementStatus.WAITING);
    }

    /**
     * Lowest denominator of the dynamic costs.
     * TODO: Investigate performant ways to assign costs to movement
     *
     * @return Cost
     */
    public double cost() {
        return 0;
    }

    @Override
    public void onTick() {
        MovementState latestState = calcState();
        Tuple<Float, Float> rotation = Utils.calcRotationFromVec3d(mc.player.getPositionEyes(1.0F),
                latestState.getGoal().rotation);
        mc.player.setPositionAndRotation(mc.player.posX, mc.player.posY, mc.player.posZ,
                rotation.getFirst(), rotation.getSecond());
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
