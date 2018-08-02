package baritone.bot.pathing.action;

import baritone.bot.Baritone;
import baritone.bot.behavior.Behavior;
import baritone.bot.pathing.action.ActionState.ActionStatus;
import baritone.bot.utils.Utils;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;

public abstract class Action extends Behavior {

    protected ActionState currentState;

    public Action(BlockPos dest) {
        BlockPos playerEyePos = new BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Tuple<Float, Float> desiredRotation = Utils.calcRotationFromCoords(
                Utils.calcCenterFromCoords(dest, world),
                playerEyePos);

        // There's honestly not a good reason for this (Builder Pattern), I just believed strongly in it
        currentState = new ActionState()
                .setGoal(new ActionState.ActionGoal(dest, desiredRotation))
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
        player.setPositionAndRotation(player.posX, player.posY, player.posZ,
                latestState.getGoal().rotation.getFirst(), latestState.getGoal().rotation.getSecond());
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
