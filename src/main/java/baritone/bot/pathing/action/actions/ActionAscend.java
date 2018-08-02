package baritone.bot.pathing.action.actions;

import baritone.bot.InputOverrideHandler;
import baritone.bot.pathing.action.Action;
import baritone.bot.pathing.action.ActionState;
import net.minecraft.util.math.BlockPos;

public class ActionAscend extends Action {

    public ActionAscend(BlockPos dest) {
        super(dest);
    }

    @Override
    public ActionState calcState() {
        ActionState latestState = currentState.setInput(InputOverrideHandler.Input.JUMP,true).setInput(InputOverrideHandler.Input.MOVE_FORWARD, true);
        if(player.getPosition().equals(latestState.getGoal().position))
            latestState.setStatus(ActionState.ActionStatus.SUCCESS);
        return latestState;
    }

}
