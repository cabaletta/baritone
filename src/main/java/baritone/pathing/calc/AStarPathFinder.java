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
import baritone.pathing.calc.openset.BinaryHeapOpenSet;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Moves;
import baritone.utils.BlockStateInterface;
import baritone.utils.Helper;
import baritone.utils.pathing.BetterWorldBorder;
import baritone.utils.pathing.MutableMoveResult;

import java.util.*;


/**
 * The actual A* pathfinding
 *
 * @author leijurv
 */
public final class AStarPathFinder extends AbstractNodeCostSearch implements Helper {

    private final Optional<HashSet<Long>> favoredPositions;
    private final CalculationContext calcContext;

    public AStarPathFinder(int startX, int startY, int startZ, Goal goal, Optional<HashSet<Long>> favoredPositions, CalculationContext context) {
        super(startX, startY, startZ, goal, context);
        this.favoredPositions = favoredPositions;
        this.calcContext = context;
    }

    @Override
    protected Optional<IPath> calculate0(long timeout) {
        startNode = getNodeAtPosition(startX, startY, startZ, BetterBlockPos.longHash(startX, startY, startZ));
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
        MutableMoveResult res = new MutableMoveResult();
        HashSet<Long> favored = favoredPositions.orElse(null);
        BetterWorldBorder worldBorder = new BetterWorldBorder(calcContext.world().getWorldBorder());
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

        long[] timeConsumed = new long[Moves.values().length];
        int[] count = new int[Moves.values().length];
        int[] stateLookup = new int[Moves.values().length];
        long[] posCreation = new long[Moves.values().length];
        long heapRemove = 0;
        int heapRemoveCount = 0;
        long heapAdd = 0;
        int heapAddCount = 0;
        long heapUpdate = 0;
        int heapUpdateCount = 0;

        long chunk = 0;
        int chunkCount = 0;

        long goalCheck = 0;
        int goalCheckCount = 0;

        long getNode = 0;
        int getNodeCount = 0;
        int startVal = BlockStateInterface.numTimesChunkSucceeded;
        int startVal2 = BlockStateInterface.numBlockStateLookups;
        long startVal3 = BetterBlockPos.numCreated;

        loopBegin();
        while (!openSet.isEmpty() && numEmptyChunk < pathingMaxChunkBorderFetch && System.nanoTime() / 1000000L - timeoutTime < 0 && !cancelRequested) {
            if (slowPath) {
                try {
                    Thread.sleep(Baritone.settings().slowPathTimeDelayMS.<Long>get());
                } catch (InterruptedException ex) {
                }
            }
            long before = System.nanoTime();
            PathNode currentNode = openSet.removeLowest();
            long t = System.nanoTime();
            heapRemove += t - before;
            heapRemoveCount++;
            currentNode.isOpen = false;
            mostRecentConsidered = currentNode;
            numNodes++;
            if (goal.isInGoal(currentNode.x, currentNode.y, currentNode.z)) {
                logDebug("Took " + (System.nanoTime() / 1000000L - startTime) + "ms, " + numMovementsConsidered + " movements considered");
                return Optional.of(new Path(startNode, currentNode, numNodes, goal, calcContext));
            }
            goalCheck += System.nanoTime() - t;
            goalCheckCount++;
            for (Moves moves : Moves.values()) {
                long s = System.nanoTime();
                int newX = currentNode.x + moves.xOffset;
                int newZ = currentNode.z + moves.zOffset;
                if ((newX >> 4 != currentNode.x >> 4 || newZ >> 4 != currentNode.z >> 4) && !calcContext.isLoaded(newX, newZ)) {
                    // only need to check if the destination is a loaded chunk if it's in a different chunk than the start of the movement
                    if (!moves.dynamicXZ) { // only increment the counter if the movement would have gone out of bounds guaranteed
                        numEmptyChunk++;
                    }
                    long costStart = System.nanoTime();
                    chunk += costStart - s;
                    chunkCount++;
                    continue;
                }
                if (!moves.dynamicXZ && !worldBorder.entirelyContains(newX, newZ)) {
                    continue;
                }
                if (currentNode.y + moves.yOffset > 256 || currentNode.y + moves.yOffset < 0) {
                    continue;
                }
                long costStart = System.nanoTime();
                chunk += costStart - s;
                chunkCount++;
                // TODO cache cost
                int numLookupsBefore = BlockStateInterface.numBlockStateLookups;
                long numCreatedBefore = BetterBlockPos.numCreated;
                res.reset();
                moves.apply(calcContext, currentNode.x, currentNode.y, currentNode.z, res);
                long costEnd = System.nanoTime();
                stateLookup[moves.ordinal()] += BlockStateInterface.numBlockStateLookups - numLookupsBefore;
                posCreation[moves.ordinal()] += BetterBlockPos.numCreated - numCreatedBefore;
                timeConsumed[moves.ordinal()] += costEnd - costStart;
                count[moves.ordinal()]++;
                numMovementsConsidered++;
                double actionCost = res.cost;
                if (actionCost >= ActionCosts.COST_INF) {
                    continue;
                }
                if (actionCost <= 0) {
                    throw new IllegalStateException(moves + " calculated implausible cost " + actionCost);
                }
                if (moves.dynamicXZ && !worldBorder.entirelyContains(res.x, res.z)) { // see issue #218
                    continue;
                }
                // check destination after verifying it's not COST_INF -- some movements return a static IMPOSSIBLE object with COST_INF and destination being 0,0,0 to avoid allocating a new result for every failed calculation
                if (!moves.dynamicXZ && (res.x != newX || res.z != newZ)) {
                    throw new IllegalStateException(moves + " " + res.x + " " + newX + " " + res.z + " " + newZ);
                }
                if (!moves.dynamicY && res.y != currentNode.y + moves.yOffset) {
                    throw new IllegalStateException(moves + " " + res.y + " " + (currentNode.y + moves.yOffset));
                }
                long hashCode = BetterBlockPos.longHash(res.x, res.y, res.z);
                if (favoring && favored.contains(hashCode)) {
                    // see issue #18
                    actionCost *= favorCoeff;
                }
                long st = System.nanoTime();
                PathNode neighbor = getNodeAtPosition(res.x, res.y, res.z, hashCode);
                getNode += System.nanoTime() - st;
                getNodeCount++;
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
                        long bef = System.nanoTime();
                        openSet.update(neighbor);
                        heapUpdate += System.nanoTime() - bef;
                        heapUpdateCount++;
                    } else {
                        long bef = System.nanoTime();
                        openSet.insert(neighbor);//dont double count, dont insert into open set if it's already there
                        heapAdd += System.nanoTime() - bef;
                        heapAddCount++;
                        neighbor.isOpen = true;
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
        int numBlockState = BlockStateInterface.numBlockStateLookups - startVal2;
        int numSucc = BlockStateInterface.numTimesChunkSucceeded - startVal;
        long numSuccc = BetterBlockPos.numCreated - startVal3;

        long totalAccountedTimeMS = 0;
        totalAccountedTimeMS += heapRemove / 1000000;
        totalAccountedTimeMS += heapAdd / 1000000;
        totalAccountedTimeMS += heapUpdate / 1000000;
        totalAccountedTimeMS += chunk / 1000000;
        totalAccountedTimeMS += getNode / 1000000;
        totalAccountedTimeMS += goalCheck / 1000000;
        System.out.println("Out of " + numBlockState + " block state lookups, " + numSucc + " were in the same chunk as the previous and could be cached");
        System.out.println("Instantiated " + numSuccc + " BetterBlockPos objects");
        System.out.println("Remove " + (heapRemove / heapRemoveCount) + " " + heapRemove / 1000000 + "ms " + heapRemoveCount);
        System.out.println("Add " + (heapAdd / heapAddCount) + " " + heapAdd / 1000000 + "ms " + heapAddCount);
        System.out.println("Update " + (heapUpdate / heapUpdateCount) + " " + heapUpdate / 1000000 + "ms " + heapUpdateCount);
        System.out.println("Chunk " + (chunk / chunkCount) + " " + chunk / 1000000 + "ms " + chunkCount);
        System.out.println("GetNode " + (getNode / getNodeCount) + " " + getNode / 1000000 + "ms " + getNodeCount);
        System.out.println("GoalCheck " + (goalCheck / goalCheckCount) + " " + goalCheck / 1000000 + "ms " + goalCheckCount);
        ArrayList<Moves> moves = new ArrayList<>(Arrays.asList(Moves.values()));
        moves.sort(Comparator.comparingLong(k -> timeConsumed[k.ordinal()] / count[k.ordinal()]));
        for (Moves move : moves) {
            int num = count[move.ordinal()];
            long nanoTime = timeConsumed[move.ordinal()];
            totalAccountedTimeMS += nanoTime / 1000000;
            System.out.println(nanoTime / num + " " + move + " " + nanoTime / 1000000 + "ms " + num);
            System.out.println(stateLookup[move.ordinal()] / num + " " + stateLookup[move.ordinal()]);
            System.out.println(posCreation[move.ordinal()] / num + " " + posCreation[move.ordinal()]);
        }
        System.out.println("Total accounted time: " + totalAccountedTimeMS);
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
                return Optional.of(new Path(startNode, bestSoFar[i], numNodes, goal, calcContext));
            }
        }
        logDebug("Even with a cost coefficient of " + COEFFICIENTS[COEFFICIENTS.length - 1] + ", I couldn't get more than " + Math.sqrt(bestDist) + " blocks");
        logDebug("No path found =(");
        return Optional.empty();
    }
}
