/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.pathfinding.goals;

import java.util.Arrays;
import java.util.Collection;
import net.minecraft.util.math.BlockPos;

/**
 *
 * @author avecowa
 */
public class GoalComposite implements Goal {
    public final Goal[] goals;
    public GoalComposite(Goal... goals) {
        this.goals = goals;
    }
    public GoalComposite(BlockPos... blocks) {
        goals = new Goal[blocks.length];
        for (int i = 0; i < blocks.length; i++) {
            goals[i] = new GoalBlock(blocks[i]);
        }
    }
    public GoalComposite(Collection<BlockPos> blocks) {
        goals = new Goal[blocks.size()];
        int i = 0;
        for (BlockPos pos : blocks) {
            goals[i] = new GoalBlock(pos);
            i++;
        }
    }
    public Goal[] goals() {
        return goals;
    }
    @Override
    public boolean isInGoal(BlockPos pos) {
        for (Goal g : goals) {
            if (g.isInGoal(pos)) {
                return true;
            }
        }
        return false;
    }
    @Override
    public double heuristic(BlockPos pos) {
        double min = Double.MAX_VALUE;
        for (Goal g : goals) {
            min = Math.min(min, g.heuristic(pos));
        }
        return min;
    }
    @Override
    public String toString() {
        return "GoalComposite" + Arrays.toString(goals);
    }
}
