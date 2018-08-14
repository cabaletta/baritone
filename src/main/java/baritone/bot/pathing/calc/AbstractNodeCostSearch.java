/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot.pathing.calc;

import baritone.bot.pathing.goals.Goal;
import baritone.bot.pathing.path.IPath;
import baritone.bot.utils.pathing.BetterBlockPos;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Any pathfinding algorithm that keeps track of nodes recursively by their cost (e.g. A*, dijkstra)
 *
 * @author leijurv
 */
public abstract class AbstractNodeCostSearch implements IPathFinder {

    /**
     * The currently running search task
     */
    protected static AbstractNodeCostSearch currentlyRunning = null;

    protected final BetterBlockPos start;

    protected final Goal goal;

    protected final Map<BetterBlockPos, PathNode> map;

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
    protected static final double[] COEFFICIENTS = {1.25, 1.5, 2, 2.5, 3, 4, 5, 10}; // big TODO tune
    /**
     * If a path goes less than 5 blocks and doesn't make it to its goal, it's not worth considering.
     */
    protected final static double MIN_DIST_PATH = 5;

    AbstractNodeCostSearch(BlockPos start, Goal goal) {
        this.start = new BetterBlockPos(start.getX(), start.getY(), start.getZ());
        this.goal = goal;
        this.map = new HashMap<>();
    }

    public synchronized Optional<IPath> calculate() {
        if (isFinished) {
            throw new IllegalStateException("Path Finder is currently in use, and cannot be reused!");
        }
        Optional<IPath> path = calculate0();
        isFinished = true;
        return path;
    }

    protected abstract Optional<IPath> calculate0();

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
    protected PathNode getNodeAtPosition(BetterBlockPos pos) {
        return map.computeIfAbsent(pos, p -> new PathNode(p, goal));
    }

    @Override
    public Optional<IPath> pathToMostRecentNodeConsidered() {
        return Optional.ofNullable(mostRecentConsidered).map(node -> new Path(startNode, node, goal, 0));
    }

    @Override
    public Optional<IPath> bestPathSoFar() {
        if (startNode == null || bestSoFar[0] == null)
            return Optional.empty();

        return Optional.of(new Path(startNode, bestSoFar[0], goal, 0));
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

    public static Optional<AbstractNodeCostSearch> getCurrentlyRunning() {
        return Optional.ofNullable(currentlyRunning);
    }
}
