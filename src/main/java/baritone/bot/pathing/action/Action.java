package baritone.bot.pathing.action;

import baritone.bot.Baritone;
import baritone.bot.event.AbstractGameEventListener;
import baritone.bot.utils.Helper;
import baritone.bot.utils.Utils;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import baritone.bot.pathing.action.ActionState.ActionStatus;
import net.minecraft.util.math.Vec3d;

public abstract class Action implements AbstractGameEventListener, Helper {

    protected ActionState currentState;

    public Action(BlockPos dest) {
        this(Utils.calcCenterFromCoords(dest, mc.world));
    }

    public Action(Vec3d dest) {
        this(dest, dest);
    }

    public Action(BlockPos dest, Vec3d rotationTarget) {
        this(Utils.calcCenterFromCoords(dest, mc.world), rotationTarget);
    }

    public Action(Vec3d dest, Vec3d rotationTarget) {
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

    /**
     * Calculate latest action state.
     * Gets called once a tick.
     *
     * @return
     */
    public abstract ActionState calcState();
}
