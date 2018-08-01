/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.pathfinding.goals;

import net.minecraft.util.math.BlockPos;

/**
 *
 * @author leijurv
 */
public interface Goal {
    /**
     * Does this position satisfy the goal?
     *
     * @param pos
     * @return
     */
    public boolean isInGoal(BlockPos pos);
    /**
     * Estimate the number of ticks it will take to get to the goal
     *
     * @param pos
     * @return
     */
    public double heuristic(BlockPos pos);
    @Override
    public String toString();
}
