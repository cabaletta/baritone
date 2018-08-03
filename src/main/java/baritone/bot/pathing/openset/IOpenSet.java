package baritone.bot.pathing.openset;

import baritone.bot.pathing.calc.PathNode;

/**
 * An open set for A* or similar graph search algorithm
 *
 * @author leijurv
 */
public interface IOpenSet {

    /**
     * Inserts the specified node into the heap
     *
     * @param node The node
     */
    void insert(PathNode node);

    /**
     * @return {@code true} if the heap has no elements; {@code false} otherwise.
     */
    boolean isEmpty();

    /**
     * Removes and returns the minimum element in the heap.
     *
     * @return The minimum element in the heap
     */
    PathNode removeLowest();
}
