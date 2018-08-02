package baritone.bot.pathing.movement.movements;

import baritone.bot.InputOverrideHandler;
import baritone.bot.pathing.movement.Movement;
import baritone.bot.pathing.movement.MovementState;
import net.minecraft.util.math.BlockPos;

public class MovementAscend extends Movement {

    public MovementAscend(BlockPos src, BlockPos dest) {
        super(src, dest);
    }

    @Override
    public MovementState calcState() {
        MovementState latestState = currentState.setInput(InputOverrideHandler.Input.JUMP, true).setInput(InputOverrideHandler.Input.MOVE_FORWARD, true);
        if (mc.player.getPosition().equals(latestState.getGoal().position))
            latestState.setStatus(MovementState.MovementStatus.SUCCESS);
        return latestState;
    }

}
