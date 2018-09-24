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
import baritone.pathing.calc.openset.BinaryHeapOpenSet;
import baritone.api.pathing.goals.Goal;
import baritone.pathing.movement.ActionCosts;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Moves;
import baritone.utils.pathing.MoveResult;
import baritone.pathing.path.IPath;
import baritone.utils.BlockStateInterface;
import baritone.utils.Helper;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Optional;

/**
 * The actual A* pathfinding
 *
 * @author leijurv
 */
public final class AStarPathFinder extends AbstractNodeCostSearch implements Helper {

    private final Optional<HashSet<Long>> favoredPositions;

    public AStarPathFinder(BlockPos start, Goal goal, Optional<HashSet<Long>> favoredPositions) {
        super(start, goal);
        this.favoredPositions = favoredPositions;
    }

    @Override
    protected Optional<IPath> calculate0(long timeout) {
        startNode = getNodeAtPosition(start.x, start.y, start.z);
        startNode.cost = 0;
        startNode.combinedCost = startNode.estimatedCostToGoal;
        BinaryHeapOpenSet openSet = new BinaryHeapOpenSet();
        openSet.insert(startNode);
        startNode.isOpen = true;
        bestSoFar = new PathNode[COEFFICIENTS.length];//keep track of the best node by the metric of (estimatedCostToGoal + cost / COEFFICIENTS[i])
        double[] bestHeuristicSoFar = new double[COEFFICIENTS.length];
        for (int i = 0; i < bestHeuristicSoFar.length; i++) {
            bestHeuristicSoFar[i] = startNode.estimatedCostToGoal;
            bestSoFar[i] = startNode;
        }
        CalculationContext calcContext = new CalculationContext();
        HashSet<Long> favored = favoredPositions.orElse(null);
        BlockStateInterface.clearCachedChunk();
        long startTime = System.nanoTime() / 1000000L;
        boolean slowPath = Baritone.settings().slowPath.get();
        if (slowPath) {
            logDebug("slowPath is on, path timeout will be " + Baritone.settings().slowPathTimeoutMS.<Long>get() + "ms instead of " + timeout + "ms");
        }
        long timeoutTime = startTime + (slowPath ? Baritone.settings().slowPathTimeoutMS.<Long>get() : timeout);
        //long lastPrintout = 0;
        int numNodes = 0;
        int numMovementsConsidered = 0;
        int numEmptyChunk = 0;
        boolean favoring = favoredPositions.isPresent();
        int pathingMaxChunkBorderFetch = Baritone.settings().pathingMaxChunkBorderFetch.get(); // grab all settings beforehand so that changing settings during pathing doesn't cause a crash or unpredictable behavior
        double favorCoeff = Baritone.settings().backtrackCostFavoringCoefficient.get();
        boolean minimumImprovementRepropagation = Baritone.settings().minimumImprovementRepropagation.get();
        loopBegin();
        while (!openSet.isEmpty() && numEmptyChunk < pathingMaxChunkBorderFetch && System.nanoTime() / 1000000L - timeoutTime < 0 && !cancelRequested) {
            if (slowPath) {
                try {
                    Thread.sleep(Baritone.settings().slowPathTimeDelayMS.<Long>get());
                } catch (InterruptedException ex) {
                }
            }
            PathNode currentNode = openSet.removeLowest();
            currentNode.isOpen = false;
            mostRecentConsidered = currentNode;
            numNodes++;
            if (goal.isInGoal(currentNode.x, currentNode.y, currentNode.z)) {
                logDebug("Took " + (System.nanoTime() / 1000000L - startTime) + "ms, " + numMovementsConsidered + " movements considered");
                return Optional.of(new Path(startNode, currentNode, numNodes, goal));
            }
            for (Moves moves : Moves.values()) {
                int newX = currentNode.x + moves.xOffset;
                int newZ = currentNode.z + moves.zOffset;
                if (newX >> 4 != currentNode.x >> 4 || newZ >> 4 != currentNode.z >> 4) {
                    // only need to check if the destination is a loaded chunk if it's in a different chunk than the start of the movement
                    if (!BlockStateInterface.isLoaded(newX, newZ)) {
                        if (!moves.dynamicXZ) { // only increment the counter if the movement would have gone out of bounds guaranteed
                            numEmptyChunk++;
                        }
                        continue;
                    }
                }
                MoveResult res = moves.apply(calcContext, currentNode.x, currentNode.y, currentNode.z);
                if (!moves.dynamicXZ && (res.destX != newX || res.destZ != newZ)) {
                    throw new IllegalStateException(moves + " " + res.destX + " " + newX + " " + res.destZ + " " + newZ);
                }
                numMovementsConsidered++;
                double actionCost = res.cost;
                if (actionCost >= ActionCosts.COST_INF) {
                    continue;
                }
                if (actionCost <= 0) {
                    throw new IllegalStateException(moves + " calculated implausible cost " + actionCost);
                }
                if (favoring && favored.contains(posHash(res.destX, res.destY, res.destZ))) {
                    // see issue #18
                    actionCost *= favorCoeff;
                }
                PathNode neighbor = getNodeAtPosition(res.destX, res.destY, res.destZ);
                double tentativeCost = currentNode.cost + actionCost;
                if (tentativeCost < neighbor.cost) {
                    if (tentativeCost < 0) {
                        throw new IllegalStateException(moves + " overflowed into negative " + actionCost + " " + neighbor.cost + " " + tentativeCost);
                    }
                    double improvementBy = neighbor.cost - tentativeCost;
                    // there are floating point errors caused by random combinations of traverse and diagonal over a flat area
                    // that means that sometimes there's a cost improvement of like 10 ^ -16
                    // it's not worth the time to update the costs, decrease-key the heap, potentially repropagate, etc
                    if (improvementBy < 0.01 && minimumImprovementRepropagation) {
                        // who cares about a hundredth of a tick? that's half a millisecond for crying out loud!
                        continue;
                    }
                    neighbor.previous = currentNode;
                    neighbor.cost = tentativeCost;
                    neighbor.combinedCost = tentativeCost + neighbor.estimatedCostToGoal;
                    if (neighbor.isOpen) {
                        openSet.update(neighbor);
                    } else {
                        neighbor.isOpen = true;
                        openSet.insert(neighbor);//dont double count, dont insert into open set if it's already there
                    }
                    for (int i = 0; i < bestSoFar.length; i++) {
                        double heuristic = neighbor.estimatedCostToGoal + neighbor.cost / COEFFICIENTS[i];
                        if (heuristic < bestHeuristicSoFar[i]) {
                            if (bestHeuristicSoFar[i] - heuristic < 0.01 && minimumImprovementRepropagation) {
                                continue;
                            }
                            bestHeuristicSoFar[i] = heuristic;
                            bestSoFar[i] = neighbor;
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
        System.out.println((int) (numNodes * 1.0 / ((System.nanoTime() / 1000000L - startTime) / 1000F)) + " nodes per second");
        double bestDist = 0;
        for (int i = 0; i < bestSoFar.length; i++) {
            if (bestSoFar[i] == null) {
                continue;
            }
            double dist = getDistFromStartSq(bestSoFar[i]);
            if (dist > bestDist) {
                bestDist = dist;
            }
            if (dist > MIN_DIST_PATH * MIN_DIST_PATH) { // square the comparison since distFromStartSq is squared
                logDebug("Took " + (System.nanoTime() / 1000000L - startTime) + "ms, A* cost coefficient " + COEFFICIENTS[i] + ", " + numMovementsConsidered + " movements considered");
                if (COEFFICIENTS[i] >= 3) {
                    System.out.println("Warning: cost coefficient is greater than three! Probably means that");
                    System.out.println("the path I found is pretty terrible (like sneak-bridging for dozens of blocks)");
                    System.out.println("But I'm going to do it anyway, because yolo");
                }
                System.out.println("Path goes for " + Math.sqrt(dist) + " blocks");
                return Optional.of(new Path(startNode, bestSoFar[i], numNodes, goal));
            }
        }
        logDebug("Even with a cost coefficient of " + COEFFICIENTS[COEFFICIENTS.length - 1] + ", I couldn't get more than " + Math.sqrt(bestDist) + " blocks");
        logDebug("No path found =(");
        return Optional.empty();
    }
}
