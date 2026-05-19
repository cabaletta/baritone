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
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.movement.ActionCosts;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.SettingsUtil;
import baritone.pathing.calc.openset.BinaryHeapOpenSet;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Moves;
import baritone.utils.pathing.BetterWorldBorder;
import baritone.utils.pathing.Favoring;
import baritone.utils.pathing.MutableMoveResult;

import java.util.Optional;

/**
 * Lazy Theta* any-angle pathfinder.
 * <p>
 * Builds on A* by performing a line-of-sight (LOS) check when a node is
 * popped from the open set. If the node has a direct LOS to its
 * grandparent (parent's parent), the parent link is rewired to skip the
 * intermediate node, producing straighter, shorter paths.
 * <p>
 * "Lazy" means LOS is only checked on extraction rather than on insertion,
 * avoiding redundant checks on nodes that may never be expanded.
 *
 * @author Ria
 * @see LineOfSight
 */
public final class LazyThetaStarPathFinder extends AbstractNodeCostSearch {

    private final Favoring favoring;
    private final CalculationContext calcContext;

    public LazyThetaStarPathFinder(BetterBlockPos realStart, int startX, int startY, int startZ,
                                    Goal goal, Favoring favoring, CalculationContext context) {
        super(realStart, startX, startY, startZ, goal, context);
        this.favoring = favoring;
        this.calcContext = context;
    }

    @Override
    protected Optional<IPath> calculate0(long primaryTimeout, long failureTimeout) {
        int minY = calcContext.world.dimensionType().minY();
        int height = calcContext.world.dimensionType().height();
        startNode = getNodeAtPosition(startX, startY, startZ, BetterBlockPos.longHash(startX, startY, startZ));
        startNode.cost = 0;
        startNode.combinedCost = startNode.estimatedCostToGoal;
        BinaryHeapOpenSet openSet = new BinaryHeapOpenSet();
        openSet.insert(startNode);
        double[] bestHeuristicSoFar = new double[COEFFICIENTS.length];
        for (int i = 0; i < bestHeuristicSoFar.length; i++) {
            bestHeuristicSoFar[i] = startNode.estimatedCostToGoal;
            bestSoFar[i] = startNode;
        }
        MutableMoveResult res = new MutableMoveResult();
        BetterWorldBorder worldBorder = new BetterWorldBorder(calcContext.world.getWorldBorder());
        long startTime = System.currentTimeMillis();
        boolean slowPath = Baritone.settings().slowPath.value;
        if (slowPath) {
            logDebug("slowPath is on, path timeout will be " + Baritone.settings().slowPathTimeoutMS.value + "ms instead of " + primaryTimeout + "ms");
        }
        long primaryTimeoutTime = startTime + (slowPath ? Baritone.settings().slowPathTimeoutMS.value : primaryTimeout);
        long failureTimeoutTime = startTime + (slowPath ? Baritone.settings().slowPathTimeoutMS.value : failureTimeout);
        boolean failing = true;
        int numNodes = 0;
        int numMovementsConsidered = 0;
        int numEmptyChunk = 0;
        boolean isFavoring = !favoring.isEmpty();
        int timeCheckInterval = 1 << 6;
        int pathingMaxChunkBorderFetch = Baritone.settings().pathingMaxChunkBorderFetch.value;
        double minimumImprovement = Baritone.settings().minimumImprovementRepropagation.value ? MIN_IMPROVEMENT : 0;
        Moves[] allMoves = Moves.values();

        // ——— Theta* pre-alloc ———————————————————————————————————————
        // Reusable positions for LOS checks to avoid allocation in the hot loop.
        BetterBlockPos nodePos = new BetterBlockPos(0, 0, 0);
        BetterBlockPos grandparentPos = new BetterBlockPos(0, 0, 0);

        while (!openSet.isEmpty() && numEmptyChunk < pathingMaxChunkBorderFetch && !cancelRequested) {
            if ((numNodes & (timeCheckInterval - 1)) == 0) {
                long now = System.currentTimeMillis();
                if (now - failureTimeoutTime >= 0 || (!failing && now - primaryTimeoutTime >= 0)) {
                    break;
                }
            }
            if (slowPath) {
                try {
                    Thread.sleep(Baritone.settings().slowPathTimeDelayMS.value);
                } catch (InterruptedException ignored) {}
            }
            PathNode currentNode = openSet.removeLowest();
            mostRecentConsidered = currentNode;
            numNodes++;

            // ——— Lazy Theta* parent rewiring ————————————————————————
            // When a node is extracted from the open set, check whether
            // it has line-of-sight to its grandparent. If so, skip the
            // intermediate parent for a straighter path.
            if (currentNode.previous != null && currentNode.previous.previous != null) {
                PathNode grandparent = currentNode.previous.previous;
                nodePos = new BetterBlockPos(currentNode.x, currentNode.y, currentNode.z);
                grandparentPos = new BetterBlockPos(grandparent.x, grandparent.y, grandparent.z);
                if (LineOfSight.hasLineOfSight(calcContext, grandparentPos, nodePos)) {
                    // Rewire: skip the parent, connect directly to grandparent
                    double grandparentToNodeCost = straightLineCost(grandparent, currentNode);
                    double newCost = grandparent.cost + grandparentToNodeCost;
                    if (newCost < currentNode.cost - minimumImprovement) {
                        currentNode.previous = grandparent;
                        currentNode.cost = newCost;
                        currentNode.combinedCost = newCost + currentNode.estimatedCostToGoal;
                    }
                }
            }

            // ——— Goal check ———————————————————————————————————————————
            if (goal.isInGoal(currentNode.x, currentNode.y, currentNode.z)) {
                logDebug("Took " + (System.currentTimeMillis() - startTime) + "ms, " + numMovementsConsidered + " movements considered");
                return Optional.of(new Path(realStart, startNode, currentNode, numNodes, goal, calcContext));
            }

            // ——— Neighbour expansion (same as A*) —————————————————————
            for (Moves moves : allMoves) {
                int newX = currentNode.x + moves.xOffset;
                int newZ = currentNode.z + moves.zOffset;
                if ((newX >> 4 != currentNode.x >> 4 || newZ >> 4 != currentNode.z >> 4) && !calcContext.isLoaded(newX, newZ)) {
                    if (!moves.dynamicXZ) {
                        numEmptyChunk++;
                    }
                    continue;
                }
                if (!moves.dynamicXZ && !worldBorder.entirelyContains(newX, newZ)) {
                    continue;
                }
                if (currentNode.y + moves.yOffset > height || currentNode.y + moves.yOffset < minY) {
                    continue;
                }
                res.reset();
                moves.apply(calcContext, currentNode.x, currentNode.y, currentNode.z, res);
                numMovementsConsidered++;
                double actionCost = res.cost;
                if (actionCost >= ActionCosts.COST_INF) {
                    continue;
                }
                if (actionCost <= 0 || Double.isNaN(actionCost)) {
                    throw new IllegalStateException(String.format(
                            "%s from %s %s %s calculated implausible cost %s",
                            moves,
                            SettingsUtil.maybeCensor(currentNode.x),
                            SettingsUtil.maybeCensor(currentNode.y),
                            SettingsUtil.maybeCensor(currentNode.z),
                            actionCost));
                }
                if (moves.dynamicXZ && !worldBorder.entirelyContains(res.x, res.z)) {
                    continue;
                }
                if (!moves.dynamicXZ && (res.x != newX || res.z != newZ)) {
                    throw new IllegalStateException(String.format(
                            "%s from %s %s %s ended at x z %s %s instead of %s %s",
                            moves,
                            SettingsUtil.maybeCensor(currentNode.x),
                            SettingsUtil.maybeCensor(currentNode.y),
                            SettingsUtil.maybeCensor(currentNode.z),
                            SettingsUtil.maybeCensor(res.x),
                            SettingsUtil.maybeCensor(res.z),
                            SettingsUtil.maybeCensor(newX),
                            SettingsUtil.maybeCensor(newZ)));
                }
                if (!moves.dynamicY && res.y != currentNode.y + moves.yOffset) {
                    throw new IllegalStateException(String.format(
                            "%s from %s %s %s ended at y %s instead of %s",
                            moves,
                            SettingsUtil.maybeCensor(currentNode.x),
                            SettingsUtil.maybeCensor(currentNode.y),
                            SettingsUtil.maybeCensor(currentNode.z),
                            SettingsUtil.maybeCensor(res.y),
                            SettingsUtil.maybeCensor(currentNode.y + moves.yOffset)));
                }
                long hashCode = BetterBlockPos.longHash(res.x, res.y, res.z);
                if (isFavoring) {
                    actionCost *= favoring.calculate(hashCode);
                }
                PathNode neighbor = getNodeAtPosition(res.x, res.y, res.z, hashCode);
                double tentativeCost = currentNode.cost + actionCost;
                if (neighbor.cost - tentativeCost > minimumImprovement) {
                    neighbor.previous = currentNode;
                    neighbor.cost = tentativeCost;
                    neighbor.combinedCost = tentativeCost + neighbor.estimatedCostToGoal;
                    if (neighbor.isOpen()) {
                        openSet.update(neighbor);
                    } else {
                        openSet.insert(neighbor);
                    }
                    for (int i = 0; i < COEFFICIENTS.length; i++) {
                        double heuristic = neighbor.estimatedCostToGoal + neighbor.cost / COEFFICIENTS[i];
                        if (bestHeuristicSoFar[i] - heuristic > minimumImprovement) {
                            bestHeuristicSoFar[i] = heuristic;
                            bestSoFar[i] = neighbor;
                            if (failing && getDistFromStartSq(neighbor) > MIN_DIST_PATH * MIN_DIST_PATH) {
                                failing = false;
                            }
                        }
                    }
                }
            }
        }
        if (cancelRequested) {
            return Optional.empty();
        }
        System.out.println(numMovementsConsidered + " movements considered");
        System.out.println("Open set size: " + openSet.size());
        System.out.println("PathNode map size: " + mapSize());
        System.out.println((int) (numNodes * 1.0 / ((System.currentTimeMillis() - startTime) / 1000F)) + " nodes per second");
        Optional<IPath> result = bestSoFar(true, numNodes);
        if (result.isPresent()) {
            logDebug("Took " + (System.currentTimeMillis() - startTime) + "ms, " + numMovementsConsidered + " movements considered");
        }
        return result;
    }

    /**
     * Estimate the movement cost along a straight line between two nodes.
     * Uses Euclidean distance multiplied by the sprint-one-block cost,
     * which is a reasonable approximation when the LOS is clear.
     */
    private static double straightLineCost(PathNode from, PathNode to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return dist * ActionCosts.WALK_ONE_BLOCK_COST;
    }
}
