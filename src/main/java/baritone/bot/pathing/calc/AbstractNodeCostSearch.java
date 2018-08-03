package baritone.bot.pathing.calc;

import baritone.bot.pathing.goals.Goal;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Any pathfinding algorithm that keeps track of nodes recursively by their cost (e.g. A*, dijkstra)
 *
 * @author leijurv
 */
public abstract class AbstractNodeCostSearch implements IPathFinder {

    /**
     * The currently running search task
     * <p>
     * TODO: This shouldn't be necessary, investigate old purpose of this field and determine necessity.
     */
    public static AbstractNodeCostSearch currentlyRunning = null;

    protected final BlockPos start;

    protected final Goal goal;

    protected final Map<BlockPos, PathNode> map;

    protected PathNode startNode;

    protected PathNode mostRecentConsidered;

    protected PathNode[] bestSoFar;

    private volatile boolean isFinished;

    /**
     * This is really complicated and hard to explain. I wrote a comment in the old version of MineBot but it was so
     * long it was easier as a Google Doc (because I could insert charts).
     *
     * @see <a href="https://docs.google.com/document/d/1WVHHXKXFdCR1Oz__KtK8sFqyvSwJN_H4lftkHFgmzlc/edit"></a>
     */
    protected static final double[] COEFFICIENTS = {1.5, 2, 2.5, 3, 4, 5, 10};
    /**
     * If a path goes less than 5 blocks and doesn't make it to its goal, it's not worth considering.
     */
    protected final static double MIN_DIST_PATH = 5;

    AbstractNodeCostSearch(BlockPos start, Goal goal) {
        this.start = start;
        this.goal = goal;
        this.map = new HashMap<>();
    }

    public synchronized IPath calculate() {
        if (isFinished) {
            throw new IllegalStateException("Path Finder is currently in use! Wait until complete to reuse!");
        }
        IPath path = calculate0();
        isFinished = true;
        return path;
    }

    protected abstract IPath calculate0();

    /**
     * Determines the distance squared from the specified node to the start
     * node. Intended for use in distance comparison, rather than anything that
     * considers the real distance value, hence the "sq".
     *
     * @param n A node
     * @return The distance, squared
     * @see AbstractNodeCostSearch#getDistFromStart(PathNode)
     */
    protected double getDistFromStartSq(PathNode n) {
        int xDiff = n.pos.getX() - start.getX();
        int yDiff = n.pos.getY() - start.getY();
        int zDiff = n.pos.getZ() - start.getZ();
        return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;
    }

    /**
     * Determines the distance from the specified node to this the node.
     *
     * @param n A node
     * @return The distance
     */
    protected double getDistFromStart(PathNode n) {
        return Math.sqrt(getDistFromStartSq(n));
    }

    /**
     * Attempts to search the {@link BlockPos} to {@link PathNode} map
     * for the node mapped to the specified pos. If no node is found,
     * a new node is created.
     *
     * @param pos The pos to lookup
     * @return The associated node
     */
    protected PathNode getNodeAtPosition(BlockPos pos) {
        return map.computeIfAbsent(pos, p -> new PathNode(p, goal));
    }

    @Override
    public IPath bestPathSoFar() {
        if (startNode == null || bestSoFar[0] == null) {
            return null;
        }
        return new Path(startNode, bestSoFar[0], goal);
    }

    @Override
    public IPath pathToMostRecentNodeConsidered() {
        return mostRecentConsidered == null ? null : new Path(startNode, mostRecentConsidered, goal);
    }

    @Override
    public final boolean isFinished() {
        return isFinished;
    }

    @Override
    public final Goal getGoal() {
        return goal;
    }

    @Override
    public final BlockPos getStart() {
        return start;
    }
}
