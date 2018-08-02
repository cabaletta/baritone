package baritone.bot.pathing.action;

import baritone.bot.Baritone;
import baritone.bot.event.AbstractGameEventListener;
import baritone.bot.pathing.action.ActionState.ActionStatus;
import baritone.bot.utils.Helper;
import baritone.bot.utils.Utils;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public abstract class Action implements AbstractGameEventListener, Helper, ActionWorldHelper {

    protected ActionState currentState;
    protected final BlockPos src;
    protected final BlockPos dest;

    protected Action(BlockPos src, BlockPos dest) {
        this(src, dest, Utils.calcCenterFromCoords(dest, mc.world));
    }


    protected Action(BlockPos src, BlockPos dest, Vec3d rotationTarget) {
        this.src = src;
        this.dest = dest;
        currentState = new ActionState()
                .setGoal(new ActionState.ActionGoal(dest, rotationTarget))
                .setStatus(ActionStatus.WAITING);
    }

    /**
     * Lowest denominator of the dynamic costs.
     * TODO: Investigate performant ways to assign costs to action
     *
     * @return Cost
     */
    public double cost() {
        return 0;
    }

    @Override
    public void onTick() {
        ActionState latestState = calcState();
        Tuple<Float, Float> rotation = Utils.calcRotationFromVec3d(mc.player.getPositionEyes(1.0F),
                latestState.getGoal().rotation);
        player.setPositionAndRotation(mc.player.posX, mc.player.posY, mc.player.posZ,
                rotation.getFirst(), rotation.getSecond());
        latestState.inputState.forEach((input, forced) -> {
            Baritone.INSTANCE.getInputOverrideHandler().setInputForceState(input, forced);
        });
        currentState = latestState;

        if (isFinished())
            return;
    }

    public boolean isFinished() {
        return (currentState.getStatus() != ActionStatus.RUNNING
                && currentState.getStatus() != ActionStatus.WAITING);
    }

    public BlockPos getSrc() {
        return src;
    }

    public BlockPos getDest() {
        return dest;
    }

    /**
     * Calculate latest action state.
     * Gets called once a tick.
     *
     * @return
     */
    public abstract ActionState calcState();
}
