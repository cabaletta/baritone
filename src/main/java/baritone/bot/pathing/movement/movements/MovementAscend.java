package baritone.bot.pathing.movement.movements;

import baritone.bot.InputOverrideHandler;
import baritone.bot.pathing.movement.Movement;
import baritone.bot.pathing.movement.MovementState;
import baritone.bot.pathing.movement.MovementState.MovementStatus;
import baritone.bot.utils.ToolSet;
import net.minecraft.util.math.BlockPos;

public class MovementAscend extends Movement {

    public MovementAscend(BlockPos src, BlockPos dest) {
        super(src, dest, new BlockPos[]{dest, src.up(2), dest.up()}, new BlockPos[]{dest.down()});
    }

    @Override
    public double calculateCost(ToolSet ts) {
        return 1;
    }

    @Override
    public void onFinish() {

    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        switch (state.getStatus()) {
            case PREPPING:
            case UNREACHABLE:
            case FAILED:
                return state;
            case WAITING:
            case RUNNING:
                MovementState latestState = state.setInput(InputOverrideHandler.Input.JUMP, true).setInput(InputOverrideHandler.Input.MOVE_FORWARD, true);
                if (playerFeet().equals(dest))
                    latestState.setStatus(MovementStatus.SUCCESS);
            default:
                return state;
        }
    }
}
