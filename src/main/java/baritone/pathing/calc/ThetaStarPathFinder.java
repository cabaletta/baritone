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
import baritone.api.utils.Pair;
import baritone.api.utils.SettingsUtil;
import baritone.pathing.calc.openset.BinaryHeapOpenSet;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.Moves;
import baritone.pathing.movement.Offset;
import baritone.utils.pathing.BetterWorldBorder;
import baritone.utils.pathing.Favoring;

import java.util.Optional;

/**
 * Theta* pathfinding - an any-angle variant of A* that creates more optimal paths
 * by using line-of-sight checks to skip intermediate nodes when possible
 *
 * @author leijurv (original A*), modified to Theta*
 */
public final class ThetaStarPathFinder extends AbstractNodeCostSearch {

    private final Favoring favoring;
    private final CalculationContext calcContext;

    public ThetaStarPathFinder(BetterBlockPos realStart, int startX, int startY, int startZ, Goal goal, Favoring favoring, CalculationContext context) {
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
        double[] bestHeuristicSoFar = new double[COEFFICIENTS.length];//keep track of the best node by the metric of (estimatedCostToGoal + cost / COEFFICIENTS[i])
        for (int i = 0; i < bestHeuristicSoFar.length; i++) {
            bestHeuristicSoFar[i] = startNode.estimatedCostToGoal;
            bestSoFar[i] = startNode;
        }
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
        int pathingMaxChunkBorderFetch = Baritone.settings().pathingMaxChunkBorderFetch.value; // grab all settings beforehand so that changing settings during pathing doesn't cause a crash or unpredictable behavior
        double minimumImprovement = Baritone.settings().minimumImprovementRepropagation.value ? MIN_IMPROVEMENT : 0;
        Moves[] allMoves = Moves.values();
        while (!openSet.isEmpty() && numEmptyChunk < pathingMaxChunkBorderFetch && !cancelRequested) {
            if ((numNodes & (timeCheckInterval - 1)) == 0) { // only call this once every 64 nodes (about half a millisecond)
                long now = System.currentTimeMillis(); // since nanoTime is slow on windows (takes many microseconds)
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

            if (goal.isInGoal(currentNode.x, currentNode.y, currentNode.z)) {
                logDebug("Took " + (System.currentTimeMillis() - startTime) + "ms, " + numMovementsConsidered + " movements considered");
                return Optional.of(new Path(realStart, startNode, currentNode, numNodes, goal, calcContext));
            }

            for (Moves move : allMoves) {
                for (Pair<Offset, Double> offsetAndCost : move.offsets(calcContext, currentNode.x, currentNode.y, currentNode.z)) {
                    int newX = currentNode.x + offsetAndCost.first().x();
                    int newY = currentNode.y + offsetAndCost.first().y();
                    int newZ = currentNode.z + offsetAndCost.first().z();

                    if ((newX >> 4 != currentNode.x >> 4 || newZ >> 4 != currentNode.z >> 4) && !calcContext.isLoaded(newX, newZ)) {
                        numEmptyChunk++;
                        continue;
                    }
                    if (!worldBorder.entirelyContains(newX, newZ)) {
                        continue;
                    }
                    if (newY > height || newY < minY) {
                        continue;
                    }

                    numMovementsConsidered++;
                    double actionCost = offsetAndCost.second();
                    if (actionCost >= ActionCosts.COST_INF) {
                        continue;
                    }
                    if (actionCost <= 0 || Double.isNaN(actionCost)) {
                        throw new IllegalStateException(String.format(
                                "%s from %s %s %s calculated implausible cost %s",
                                move,
                                SettingsUtil.maybeCensor(currentNode.x),
                                SettingsUtil.maybeCensor(currentNode.y),
                                SettingsUtil.maybeCensor(currentNode.z),
                                actionCost));
                    }

                    long hashCode = BetterBlockPos.longHash(newX, newY, newZ);

                    PathNode neighbor = getNodeAtPosition(newX, newY, newZ, hashCode);

                    PathNode parentNode = currentNode.previous;
                    double tentativeCost;

                    double shortcutCost = -1;

                    if (parentNode != null && (shortcutCost = move.cost(calcContext, parentNode.x, parentNode.y, parentNode.z, newX, newY, newZ)) <= currentNode.cost + actionCost) {
                        // Theta Star
                        actionCost = shortcutCost;
                        tentativeCost = parentNode.cost;
                    } else {
                        if (parentNode != null) {
                            HELPER.logDebug("Shortcut: " + shortcutCost);
                            HELPER.logDebug("Lame: " + parentNode.cost + actionCost);
                        }
                        parentNode = currentNode;
                        tentativeCost = currentNode.cost;
                    }

                    if (isFavoring) {
                        actionCost *= favoring.calculate(hashCode);
                    }
                    tentativeCost += actionCost;

                    if (neighbor.cost - tentativeCost > minimumImprovement) {
                        neighbor.previous = parentNode;
                        neighbor.cost = tentativeCost;
                        neighbor.combinedCost = tentativeCost + neighbor.estimatedCostToGoal;

                        if (neighbor.isOpen()) {
                            openSet.update(neighbor);
                        } else {
                            openSet.insert(neighbor); //don't double count, don't insert into the open set if it's already there
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
}