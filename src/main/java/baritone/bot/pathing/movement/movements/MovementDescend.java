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

public class MovementDescend extends Movement {
    public MovementDescend(BlockPos start, BlockPos end) {
        super(start, end, new BlockPos[]{end.up(2), end.up(), end}, new BlockPos[]{end.down()});
    }

    @Override
    protected double calculateCost(ToolSet ts) {
        if (!MovementHelper.canWalkOn(positionsToPlace[0], BlockStateInterface.get(positionsToPlace[0]))) {
            return COST_INF;
        }
        Block tmp1 = BlockStateInterface.get(dest).getBlock();
        if (tmp1 instanceof BlockLadder || tmp1 instanceof BlockVine) {
            return COST_INF;
        }
        return WALK_ONE_BLOCK_COST * 0.8 + Math.max(FALL_N_BLOCKS_COST[1], WALK_ONE_BLOCK_COST * 0.2) + getTotalHardnessOfBlocksToBreak(ts);//we walk half the block plus 0.3 to get to the edge, then we walk the other 0.2 while simultaneously falling (math.max because of how it's in parallel)
    }

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
                BlockPos playerFeet = playerFeet();

                if (playerFeet.equals(dest) && player().posY - playerFeet.getY() < 0.01) {
                    // Wait until we're actually on the ground before saying we're done because sometimes we continue to fall if the next action starts immediately
                    state.setStatus(MovementState.MovementStatus.SUCCESS);
                    return state;
                }
                Rotation rotationToBlock = Utils.calcRotationFromVec3d(playerHead(), Utils.calcCenterFromCoords(positionsToBreak[1], world()));
                return state.setTarget(new MovementState.MovementTarget(rotationToBlock)).setInput(InputOverrideHandler.Input.MOVE_FORWARD, true);
            default:
                return state;
        }
    }

}
