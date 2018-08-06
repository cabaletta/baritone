package baritone.bot.pathing.movement.movements;

import baritone.bot.InputOverrideHandler;
import baritone.bot.pathing.movement.Movement;
import baritone.bot.pathing.movement.MovementHelper;
import baritone.bot.pathing.movement.MovementState;
import baritone.bot.utils.BlockStateInterface;
import baritone.bot.utils.Rotation;
import baritone.bot.utils.ToolSet;
import baritone.bot.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockVine;
import net.minecraft.util.math.BlockPos;

public class MovementDownward extends Movement {
    public MovementDownward(BlockPos start) {
        super(start, start.down(), new BlockPos[]{start.down()}, new BlockPos[0]);
    }

    int numTicks = 0;

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        System.out.println("Ticking with state " + state.getStatus());
        switch (state.getStatus()) {
            case PREPPING:
            case UNREACHABLE:
            case FAILED:
                return state;
            case WAITING:
            case RUNNING:
                if (playerFeet().equals(dest)) {
                    state.setStatus(MovementState.MovementStatus.SUCCESS);
                    return state;
                }
                if (numTicks++ < 10) {
                    return state;
                }
                Rotation rotationToBlock = Utils.calcRotationFromVec3d(playerHead(), Utils.calcCenterFromCoords(positionsToBreak[0], world()));
                return state.setTarget(new MovementState.MovementTarget(rotationToBlock)).setInput(InputOverrideHandler.Input.MOVE_FORWARD, true);
            default:
                return state;
        }
    }

    @Override
    protected double calculateCost(ToolSet ts) {
        if (!MovementHelper.canWalkOn(dest.down(), BlockStateInterface.get(dest.down()))) {
            return COST_INF;
        }
        Block td = BlockStateInterface.get(dest).getBlock();
        boolean ladder = td instanceof BlockLadder || td instanceof BlockVine;
        if (ladder) {
            return LADDER_DOWN_ONE_COST;
        } else {
            return FALL_N_BLOCKS_COST[1] + getTotalHardnessOfBlocksToBreak(ts);
        }
    }
}
