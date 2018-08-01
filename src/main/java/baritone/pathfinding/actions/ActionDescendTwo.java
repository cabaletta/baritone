/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.pathfinding.actions;

import baritone.Baritone;
import baritone.movement.MovementManager;
import baritone.util.ToolSet;
import net.minecraft.util.math.BlockPos;

/**
 *
 * @author leijurv
 */
public class ActionDescendTwo extends ActionPlaceOrBreak {

    public ActionDescendTwo(BlockPos start, BlockPos end) {
        super(start, end, new BlockPos[]{end.up(3), end.up(2), end.up(), end}, new BlockPos[]{end.down()});
    }

    @Override
    protected double calculateCost(ToolSet ts) {
        if (!canWalkOn(positionsToPlace[0])) {
            return COST_INF;
        }
        if (getTotalHardnessOfBlocksToBreak(ts) != 0) {
            return COST_INF;
        }
        return WALK_ONE_BLOCK_COST * 0.8 + Math.max(FALL_TWO_BLOCK_COST, WALK_ONE_BLOCK_COST * 0.2);//we walk half the block plus 0.3 to get to the edge, then we walk the other 0.2 while simultaneously falling (math.max because of how it's in parallel)
    }

    @Override
    protected boolean tick0() {//basically just hold down W until we are where we want to be
        MovementManager.moveTowardsBlock(to);
        return Baritone.playerFeet.equals(to);
    }
}
