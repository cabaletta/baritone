/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.pathfinding.actions;

import baritone.movement.MovementManager;
import baritone.util.ToolSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockVine;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

/**
 *
 * @author leijurv
 */
public class ActionDescend extends ActionPlaceOrBreak {
    public ActionDescend(BlockPos start, BlockPos end) {
        super(start, end, new BlockPos[]{end.up(2), end.up(), end}, new BlockPos[]{end.down()});
    }
    @Override
    protected double calculateCost(ToolSet ts) {
        if (!canWalkOn(positionsToPlace[0])) {
            return COST_INF;
        }
        Block tmp1 = Minecraft.getMinecraft().world.getBlockState(to).getBlock();
        if (tmp1 instanceof BlockLadder || tmp1 instanceof BlockVine) {
            return COST_INF;
        }
        return WALK_ONE_BLOCK_COST * 0.8 + Math.max(FALL_ONE_BLOCK_COST, WALK_ONE_BLOCK_COST * 0.2) + getTotalHardnessOfBlocksToBreak(ts);//we walk half the block plus 0.3 to get to the edge, then we walk the other 0.2 while simultaneously falling (math.max because of how it's in parallel)
    }
    @Override
    protected boolean tick0() {//basically just hold down W until we are where we want to be
        MovementManager.moveTowardsBlock(to);
        return Minecraft.getMinecraft().player.getPosition0().equals(to);
    }
}
