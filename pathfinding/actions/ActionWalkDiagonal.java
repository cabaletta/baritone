/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.pathfinding.actions;

import java.util.Random;
import baritone.Baritone;
import baritone.movement.MovementManager;
import baritone.ui.LookManager;
import baritone.util.ToolSet;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;

/**
 *
 * @author leijurv
 */
public class ActionWalkDiagonal extends ActionPlaceOrBreak {
    public ActionWalkDiagonal(BlockPos start, EnumFacing dir1, EnumFacing dir2) {
        this(start, start.offset(dir1), start.offset(dir2), dir2);
        //super(start, start.offset(dir1).offset(dir2), new BlockPos[]{start.offset(dir1), start.offset(dir1).up(), start.offset(dir2), start.offset(dir2).up(), start.offset(dir1).offset(dir2), start.offset(dir1).offset(dir2).up()}, new BlockPos[]{start.offset(dir1).offset(dir2).down()});
    }
    public ActionWalkDiagonal(BlockPos start, BlockPos dir1, BlockPos dir2, EnumFacing drr2) {
        this(start, dir1.offset(drr2), dir1, dir2);
    }
    public ActionWalkDiagonal(BlockPos start, BlockPos end, BlockPos dir1, BlockPos dir2) {
        super(start, end, new BlockPos[]{dir1, dir1.up(), dir2, dir2.up(), end, end.up()}, new BlockPos[]{end.down()});
    }
    private Boolean oneInTen = null;
    @Override
    protected boolean tick0() {
        if (oneInTen == null) {
            oneInTen = new Random().nextInt(10) == 0;
        }
        if (oneInTen) {
            MovementManager.forward = LookManager.lookAtBlock(to, false);
        } else {
            MovementManager.moveTowardsBlock(to);
        }
        if (MovementManager.forward && !MovementManager.backward) {
            Minecraft.getMinecraft().player.setSprinting(true);
        }
        return to.equals(Minecraft.getMinecraft().player.getPosition0());
    }
    @Override
    protected double calculateCost(ToolSet ts) {
        if (!Baritone.allowDiagonal) {
            return COST_INF;
        }
        if (getTotalHardnessOfBlocksToBreak(ts) != 0) {
            return COST_INF;
        }
        if (!canWalkOn(positionsToPlace[0])) {
            return COST_INF;
        }
        return Math.sqrt(2) * (isWater(from) || isWater(to) ? WALK_ONE_IN_WATER_COST : WALK_ONE_BLOCK_COST);
    }
}
