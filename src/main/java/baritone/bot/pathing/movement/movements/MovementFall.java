package baritone.bot.pathing.movement.movements;

import baritone.bot.pathing.movement.ActionCosts;
import baritone.bot.pathing.movement.Movement;
import baritone.bot.pathing.movement.MovementHelper;
import baritone.bot.utils.BlockStateInterface;
import baritone.bot.utils.ToolSet;
import net.minecraft.util.math.BlockPos;

public class MovementFall extends Movement {

    private static BlockPos[] buildPositionsToBreak(BlockPos src, BlockPos dest) {
        BlockPos[] toBreak;
        int diffX = dest.getX() - src.getX();
        int diffZ = dest.getZ() - src.getZ();
        int diffY = dest.getY() - src.getY();
        toBreak = new BlockPos[diffY + 1];
        for(int i = 0; i < toBreak.length; i++ ) {
            toBreak[i] = new BlockPos(dest.getX() - diffX, dest.getY() + (toBreak.length - 1) - i, dest.getZ() - diffZ);
        }
        return toBreak;
    }


    protected MovementFall(BlockPos src, BlockPos dest) {
        super(src, dest, MovementFall.buildPositionsToBreak(src, dest), new BlockPos[]{dest.down()});
    }

    @Override
    protected double calculateCost(ToolSet ts) {
        if(!MovementHelper.canWalkOn(positionsToPlace[0], BlockStateInterface.get(positionsToPlace[0]))) {
            return COST_INF;
        }
        double placeBucketCost = 0.0;
        if(!BlockStateInterface.isWater(dest)) {
            placeBucketCost = ActionCosts.PLACE_ONE_BLOCK_COST;
        }
        return WALK_OFF_BLOCK_COST + FALL_N_BLOCKS_COST[positionsToBreak.length - 1] + MovementHelper.getTotalHardnessOfBlocksToBreak(ts, positionsToBreak) + placeBucketCost;
    }

}
