package baritone.bot.pathing.calc;

import baritone.bot.pathing.goals.Goal;
import net.minecraft.util.math.BlockPos;

/**
 * Generic path finder interface
 *
 * @author leijurv
 */
public interface IPathFinder {

    BlockPos getStart();

    Goal getGoal();

    /**
     * Calculate the path in full. Will take several seconds.
     *
     * @return The final path
     */
    IPath calculate();

    /**
     * Intended to be called concurrently with calculatePath from a different thread to tell if it's finished yet
     *
     * @return Whether or not this finder is finished
     */
    boolean isFinished();

    /**
     * Called for path rendering. Returns a path to the most recent node popped from the open set and considered.
     *
     * @return The temporary path
     */
    IPath pathToMostRecentNodeConsidered();

    /**
     * The best path so far, according to the most forgiving coefficient heuristic (the reason being that that path is
     * most likely to represent the true shape of the path to the goal, assuming it's within a possible cost heuristic.
     * That's almost always a safe assumption, but in the case of a nearly impossible path, it still works by providing
     * a theoretically plausible but practically unlikely path)
     *
     * @return The temporary path
     */
    IPath bestPathSoFar();
}
