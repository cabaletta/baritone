/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.pathfinding.goals;

import baritone.pathfinding.actions.Action;
import net.minecraft.util.math.BlockPos;

/**
 *
 * @author leijurv
 */
public class GoalXZ implements Goal {
    final int x;
    final int z;
    public GoalXZ(int x, int z) {
        this.x = x;
        this.z = z;
    }
    @Override
    public boolean isInGoal(BlockPos pos) {
        return pos.getX() == x && pos.getZ() == z;
    }
    @Override
    public double heuristic(BlockPos pos) {//mostly copied from GoalBlock
        double xDiff = pos.getX() - this.x;
        double zDiff = pos.getZ() - this.z;
        return calculate(xDiff, zDiff);
    }
    public static double calculate(double xDiff, double zDiff) {
        return calculate(xDiff, zDiff, 0);
    }
    /*
     public static double calculate(double xDiff, double zDiff) {
     double pythaDist = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
     return calculate(xDiff, zDiff, pythaDist);
     }
     public static double calculateOld(double xDiff, double zDiff, double pythaDist) {
     double heuristic = 0;
     heuristic += Math.abs(xDiff) * Action.WALK_ONE_BLOCK_COST * 1.1;//overestimate
     heuristic += Math.abs(zDiff) * Action.WALK_ONE_BLOCK_COST * 1.1;
     heuristic += pythaDist / 10 * Action.WALK_ONE_BLOCK_COST;
     return heuristic;
     }
     */
    static final double sq = Math.sqrt(2);
    public static double calculate(double xDiff, double zDiff, double pythaDist) {
        double x = Math.abs(xDiff);
        double z = Math.abs(zDiff);
        double straight;
        double diagonal;
        if (x < z) {
            straight = z - x;
            diagonal = x;
        } else {
            straight = x - z;
            diagonal = z;
        }
        diagonal *= sq;
        return (diagonal + straight) * Action.WALK_ONE_BLOCK_COST;
    }
    @Override
    public String toString() {
        return "Goal{x=" + x + ",z=" + z + "}";
    }
}
