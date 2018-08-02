/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.bot.pathing.goals;

import baritone.bot.pathing.actions.ActionCosts;
import net.minecraft.util.math.BlockPos;

/**
 * An abstract Goal for pathing, can be anything from a specific block to just a Y coordinate.
 * @author leijurv
 */
public interface Goal extends ActionCosts {

    /**
     * Returns whether or not the specified position
     * meets the requirement for this goal based.
     *
     * @param pos The position
     * @return Whether or not it satisfies this goal
     */
    boolean isInGoal(BlockPos pos);

    /**
     * Estimate the number of ticks it will take to get to the goal
     *
     * @param pos The
     * @return The estimate number of ticks to satisfy the goal
     */
    double heuristic(BlockPos pos);
}
