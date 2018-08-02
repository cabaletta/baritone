package baritone.pathfinding.actions;

import baritone.Baritone;
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
public class ActionFall extends ActionPlaceOrBreak {
    public ActionFall(BlockPos start) {
        super(start, start.down(), new BlockPos[]{start.down()}, new BlockPos[0]);
    }
    int numTicks = 0;
    @Override
    protected boolean tick0() {
        numTicks++;
        if (numTicks > 10) {
            MovementManager.moveTowardsBlock(to);
        }
        return Baritone.playerFeet.equals(to);
    }
    @Override
    protected double calculateCost(ToolSet ts) {
        if (!Baritone.allowVerticalMotion || !canWalkOn(to.down())) {
            return COST_INF;
        }
        Block td = Baritone.get(to).getBlock();
        boolean ladder = td instanceof BlockLadder || td instanceof BlockVine;
        if (ladder) {
            return LADDER_DOWN_ONE_COST;
        } else {
            return FALL_ONE_BLOCK_COST + getTotalHardnessOfBlocksToBreak(ts);
        }
    }
}
