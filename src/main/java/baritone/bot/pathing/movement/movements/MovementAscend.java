package baritone.bot.pathing.movement.movements;

import baritone.bot.InputOverrideHandler;
import baritone.bot.pathing.movement.Movement;
import baritone.bot.pathing.movement.MovementHelper;
import baritone.bot.pathing.movement.MovementState;
import baritone.bot.pathing.movement.MovementState.MovementStatus;
import baritone.bot.utils.BlockStateInterface;
import baritone.bot.utils.ToolSet;
import baritone.bot.utils.Utils;
import net.minecraft.block.BlockFalling;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class MovementAscend extends Movement {
    BlockPos[] against = new BlockPos[3];

    public MovementAscend(BlockPos src, BlockPos dest) {
        super(src, dest, new BlockPos[]{dest, src.up(2), dest.up()}, new BlockPos[]{dest.down()});

        BlockPos placementLocation = positionsToPlace[0];//end.down()
        int i = 0;
        if (!placementLocation.north().equals(src)) {
            against[i] = placementLocation.north();
            i++;
        }
        if (!placementLocation.south().equals(src)) {
            against[i] = placementLocation.south();
            i++;
        }
        if (!placementLocation.east().equals(src)) {
            against[i] = placementLocation.east();
            i++;
        }
        if (!placementLocation.west().equals(src)) {
            against[i] = placementLocation.west();
            i++;
        }
        //TODO: add ability to place against .down() as well as the cardinal directions
        //useful for when you are starting a staircase without anything to place against
        // Counterpoint to the above TODO ^ you should move then pillar instead of ascend
    }

    @Override
    protected double calculateCost(ToolSet ts) {
        if (!MovementHelper.canWalkOn(positionsToPlace[0], BlockStateInterface.get(positionsToPlace[0]))) {
            if (!MovementHelper.isAir(positionsToPlace[0]) && !MovementHelper.isWater(positionsToPlace[0])) {
                return COST_INF;
            }
            for (BlockPos against1 : against) {
                if (BlockStateInterface.get(against1).isBlockNormalCube()) {
                    return JUMP_ONE_BLOCK_COST + WALK_ONE_BLOCK_COST + PLACE_ONE_BLOCK_COST + getTotalHardnessOfBlocksToBreak(ts);
                }
            }
            return COST_INF;
        }
        if (BlockStateInterface.get(src.up(3)).getBlock() instanceof BlockFalling) {//it would fall on us and possibly suffocate us
            return COST_INF;
        }
        return WALK_ONE_BLOCK_COST / 2 + Math.max(JUMP_ONE_BLOCK_COST, WALK_ONE_BLOCK_COST / 2) + getTotalHardnessOfBlocksToBreak(ts);//we walk half the block to get to the edge, then we walk the other half while simultaneously jumping (math.max because of how it's in parallel)
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
                if (playerFeet().equals(dest)) {
                    state.setStatus(MovementStatus.SUCCESS);
                    return state;
                }

                state.setTarget(new MovementState.MovementTarget(Optional.empty(), Optional.of(Utils.calcRotationFromVec3d(new Vec3d(player().posX, player().posY + 1.62, player().posZ), Utils.calcCenterFromCoords(positionsToBreak[0], world())))));
                state.setInput(InputOverrideHandler.Input.JUMP, true).setInput(InputOverrideHandler.Input.MOVE_FORWARD, true);
                return state;
            default:
                return state;
        }
    }
}
