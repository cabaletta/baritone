/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.pathing.calc;

import baritone.Baritone;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.calc.IPathFinder;
import baritone.api.pathing.goals.Goal;
import baritone.api.utils.PathCalculationResult;
import baritone.pathing.movement.CalculationContext;
import baritone.utils.Helper;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.Optional;

/**
 * Any pathfinding algorithm that keeps track of nodes recursively by their cost (e.g. A*, dijkstra)
 *
 * @author leijurv
 */
public abstract class AbstractNodeCostSearch implements IPathFinder {

    protected final int startX;
    protected final int startY;
    protected final int startZ;

    protected final Goal goal;

    private final CalculationContext context;

    /**
     * @see <a href="https://github.com/cabaletta/baritone/issues/107">Issue #107</a>
     */
    private final Long2ObjectOpenHashMap<PathNode> map;

    protected PathNode startNode;

    protected PathNode mostRecentConsidered;

    protected PathNode[] bestSoFar;

    private volatile boolean isFinished;

    protected boolean cancelRequested;

    /**
     * This is really complicated and hard to explain. I wrote a comment in the old version of MineBot but it was so
     * long it was easier as a Google Doc (because I could insert charts).
     *
     * @see <a href="https://docs.google.com/document/d/1WVHHXKXFdCR1Oz__KtK8sFqyvSwJN_H4lftkHFgmzlc/edit"></a>
     */
    protected static final double[] COEFFICIENTS = {1.5, 2, 2.5, 3, 4, 5, 10}; // big TODO tune
    /**
     * If a path goes less than 5 blocks and doesn't make it to its goal, it's not worth considering.
     */
    protected final static double MIN_DIST_PATH = 5;

    AbstractNodeCostSearch(int startX, int startY, int startZ, Goal goal, CalculationContext context) {
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.goal = goal;
        this.context = context;
        this.map = new Long2ObjectOpenHashMap<>(Baritone.settings().pathingMapDefaultSize.value, Baritone.settings().pathingMapLoadFactor.get());
    }

    public void cancel() {
        cancelRequested = true;
    }

    public synchronized PathCalculationResult calculate(long timeout) {
        if (isFinished) {
            throw new IllegalStateException("Path Finder is currently in use, and cannot be reused!");
        }
        this.cancelRequested = false;
        try {
            Optional<IPath> path = calculate0(timeout);
            path = path.map(IPath::postProcess);
            isFinished = true;
            if (cancelRequested) {
                return new PathCalculationResult(PathCalculationResult.Type.CANCELLATION, path);
            }
            if (!path.isPresent()) {
                return new PathCalculationResult(PathCalculationResult.Type.FAILURE, path);
            }
            if (goal.isInGoal(path.get().getDest())) {
                return new PathCalculationResult(PathCalculationResult.Type.SUCCESS_TO_GOAL, path);
            } else {
                return new PathCalculationResult(PathCalculationResult.Type.SUCCESS_SEGMENT, path);
            }
        } catch (Exception e) {
            Helper.HELPER.logDebug("Pathing exception: " + e);
            e.printStackTrace();
            return new PathCalculationResult(PathCalculationResult.Type.EXCEPTION, Optional.empty());
        } finally {
            // this is run regardless of what exception may or may not be raised by calculate0
            isFinished = true;
        }
    }

    protected abstract Optional<IPath> calculate0(long timeout);

    /**
     * Determines the distance squared from the specified node to the start
     * node. Intended for use in distance comparison, rather than anything that
     * considers the real distance value, hence the "sq".
     *
     * @param n A node
     * @return The distance, squared
     */
    protected double getDistFromStartSq(PathNode n) {
        int xDiff = n.x - startX;
        int yDiff = n.y - startY;
        int zDiff = n.z - startZ;
        return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;
    }

    /**
     * Attempts to search the block position hashCode long to {@link PathNode} map
     * for the node mapped to the specified pos. If no node is found,
     * a new node is created.
     *
     * @return The associated node
     * @see <a href="https://github.com/cabaletta/baritone/issues/107">Issue #107</a>
     */
    protected PathNode getNodeAtPosition(int x, int y, int z, long hashCode) {
        PathNode node = map.get(hashCode);
        if (node == null) {
            node = new PathNode(x, y, z, goal);
            map.put(hashCode, node);
        }
        return node;
    }

    @Override
    public Optional<IPath> pathToMostRecentNodeConsidered() {
        return Optional.ofNullable(mostRecentConsidered).map(node -> new Path(startNode, node, 0, goal, context));
    }

    protected int mapSize() {
        return map.size();
    }

    @Override
    public Optional<IPath> bestPathSoFar() {
        if (startNode == null || bestSoFar == null || bestSoFar[0] == null) {
            return Optional.empty();
        }
        for (int i = 0; i < bestSoFar.length; i++) {
            if (bestSoFar[i] == null) {
                continue;
            }
            if (getDistFromStartSq(bestSoFar[i]) > MIN_DIST_PATH * MIN_DIST_PATH) { // square the comparison since distFromStartSq is squared
                try {
                    return Optional.of(new Path(startNode, bestSoFar[i], 0, goal, context));
                } catch (IllegalStateException ex) {
                    System.out.println("Unable to construct path to render");
                    return Optional.empty();
                }
            }
        }
        // instead of returning bestSoFar[0], be less misleading
        // if it actually won't find any path, don't make them think it will by rendering a dark blue that will never actually happen
        return Optional.empty();
    }

    @Override
    public final boolean isFinished() {
        return isFinished;
    }

    @Override
    public final Goal getGoal() {
        return goal;
    }
}
