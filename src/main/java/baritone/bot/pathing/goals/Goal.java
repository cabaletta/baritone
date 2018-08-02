/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.bot.pathing.goals;

import net.minecraft.util.math.BlockPos;

/**
 * An abstract Goal for pathing, can be anything from a specific block to just a Y coordinate.
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

    /**
     * Summarize the goal
     * @return
     */
    @Override
    public String toString();
}
