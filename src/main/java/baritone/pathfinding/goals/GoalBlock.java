/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.pathfinding.goals;

import baritone.Baritone;
import baritone.pathfinding.actions.Action;
import net.minecraft.util.math.BlockPos;

/**
 * A specific BlockPos goal
 * @author leijurv
 */
public class GoalBlock implements Goal {

    final int x, y, z;

    public GoalBlock() {
        this(Baritone.playerFeet);
    }

    public GoalBlock(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public GoalBlock(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BlockPos pos() {
        return new BlockPos(x, y, z);
    }

    @Override
    public boolean isInGoal(BlockPos pos) {
        return pos.getX() == this.x && pos.getY() == this.y && pos.getZ() == this.z;
    }
    static final double MIN = 20;
    static final double MAX = 150;

    @Override
    public double heuristic(BlockPos pos) {
        double xDiff = pos.getX() - this.x;
        double yDiff = pos.getY() - this.y;
        double zDiff = pos.getZ() - this.z;
        return calculate(xDiff, yDiff, zDiff);
    }

    public static double calculate(double xDiff, double yDiff, double zDiff) {
        double pythaDist = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
        double heuristic = 0;
        double baseline = (Action.PLACE_ONE_BLOCK_COST + Action.FALL_ONE_BLOCK_COST) * 32;
        if (pythaDist < MAX) {//if we are more than MAX away, ignore the Y coordinate. It really doesn't matter how far away your Y coordinate is if you X coordinate is 1000 blocks away.
            double multiplier = pythaDist < MIN ? 1 : 1 - (pythaDist - MIN) / (MAX - MIN);
            if (yDiff < 0) {//pos.getY()-this.y<0 therefore pos.getY()<this.y, so target is above current
                heuristic -= yDiff * (Action.PLACE_ONE_BLOCK_COST * 0.7 + Action.JUMP_ONE_BLOCK_COST);//target above current
            } else {
                heuristic += yDiff * (10 + Action.FALL_ONE_BLOCK_COST);//target below current
            }
            heuristic *= multiplier;
            heuristic += (1 - multiplier) * baseline;
        } else {
            heuristic += baseline;
        }
        heuristic += GoalXZ.calculate(xDiff, zDiff, pythaDist);
        return heuristic;
    }

    @Override
    public String toString() {
        return "Goal{x=" + x + ",y=" + y + ",z=" + z + "}";
    }
}
