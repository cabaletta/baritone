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

package baritone.pathing.calc;

import baritone.Baritone;
import baritone.chunk.CachedWorld;
import baritone.chunk.WorldProvider;
import baritone.pathing.calc.openset.BinaryHeapOpenSet;
import baritone.pathing.calc.openset.IOpenSet;
import baritone.pathing.goals.Goal;
import baritone.pathing.movement.ActionCosts;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.movements.*;
import baritone.pathing.path.IPath;
import baritone.utils.Helper;
import baritone.utils.pathing.BetterBlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.EmptyChunk;

import java.util.*;

/**
 * The actual A* pathfinding
 *
 * @author leijurv
 */
public class AStarPathFinder extends AbstractNodeCostSearch implements Helper {

    private final Optional<HashSet<BetterBlockPos>> favoredPositions;

    public AStarPathFinder(BlockPos start, Goal goal, Optional<Collection<BetterBlockPos>> favoredPositions) {
        super(start, goal);
        this.favoredPositions = favoredPositions.map(HashSet::new); // <-- okay this is epic
    }

    @Override
    protected Optional<IPath> calculate0() {
        startNode = getNodeAtPosition(start);
        startNode.cost = 0;
        startNode.combinedCost = startNode.estimatedCostToGoal;
        IOpenSet openSet = new BinaryHeapOpenSet();
        openSet.insert(startNode);
        startNode.isOpen = true;
        bestSoFar = new PathNode[COEFFICIENTS.length];//keep track of the best node by the metric of (estimatedCostToGoal + cost / COEFFICIENTS[i])
        double[] bestHeuristicSoFar = new double[COEFFICIENTS.length];
        for (int i = 0; i < bestHeuristicSoFar.length; i++) {
            bestHeuristicSoFar[i] = Double.MAX_VALUE;
        }
        CalculationContext calcContext = new CalculationContext();
        HashSet<BetterBlockPos> favored = favoredPositions.orElse(null);
        currentlyRunning = this;
        CachedWorld world = Optional.ofNullable(WorldProvider.INSTANCE.getCurrentWorld()).map(w -> w.cache).orElse(null);
        long startTime = System.currentTimeMillis();
        boolean slowPath = Baritone.settings().slowPath.get();
        long timeoutTime = startTime + (slowPath ? Baritone.settings().slowPathTimeoutMS : Baritone.settings().pathTimeoutMS).<Long>get();
        long lastPrintout = 0;
        int numNodes = 0;
        int numMovementsConsidered = 0;
        int numEmptyChunk = 0;
        boolean favoring = favoredPositions.isPresent();
        int pathingMaxChunkBorderFetch = Baritone.settings().pathingMaxChunkBorderFetch.get(); // grab all settings beforehand so that changing settings during pathing doesn't cause a crash or unpredictable behavior
        double favorCoeff = Baritone.settings().backtrackCostFavoringCoefficient.get();
        boolean minimumImprovementRepropagation = Baritone.settings().minimumImprovementRepropagation.get();
        HashMap<Class<? extends Movement>, Long> timeConsumed = new HashMap<>();
        HashMap<Class<? extends Movement>, Integer> count = new HashMap<>();
        long heapRemove = 0;
        int heapRemoveCount = 0;
        long heapAdd = 0;
        int heapAddCount = 0;
        long heapUpdate = 0;
        int heapUpdateCount = 0;

        long construction = 0;
        int constructionCount = 0;

        long chunk = 0;
        int chunkCount = 0;

        long chunk2 = 0;
        int chunkCount2 = 0;

        long getNode = 0;
        int getNodeCount = 0;
        while (!openSet.isEmpty() && numEmptyChunk < pathingMaxChunkBorderFetch && System.currentTimeMillis() < timeoutTime && !cancelRequested) {
            if (slowPath) {
                try {
                    Thread.sleep(Baritone.settings().slowPathTimeDelayMS.<Long>get());
                } catch (InterruptedException ex) {
                }
            }
            long before = System.nanoTime();
            PathNode currentNode = openSet.removeLowest();
            heapRemove += System.nanoTime() - before;
            heapRemoveCount++;
            currentNode.isOpen = false;
            mostRecentConsidered = currentNode;
            BetterBlockPos currentNodePos = currentNode.pos;
            numNodes++;
            if (System.currentTimeMillis() > lastPrintout + 1000) {//print once a second
                System.out.println("searching... at " + currentNodePos + ", considered " + numNodes + " nodes so far");
                lastPrintout = System.currentTimeMillis();
            }
            if (goal.isInGoal(currentNodePos)) {
                currentlyRunning = null;
                return Optional.of(new Path(startNode, currentNode, numNodes));
            }
            long constructStart = System.nanoTime();
            Movement[] possibleMovements = getConnectedPositions(currentNodePos, calcContext);//movement that we could take that start at currentNodePos, in random order
            shuffle(possibleMovements);
            long constructEnd = System.nanoTime();
            construction += constructEnd - constructStart;
            constructionCount++;
            //System.out.println(constructEnd - constructStart);
            for (Movement movementToGetToNeighbor : possibleMovements) {
                if (movementToGetToNeighbor == null) {
                    continue;
                }
                BetterBlockPos dest = (BetterBlockPos) movementToGetToNeighbor.getDest();
                long s = System.nanoTime();
                boolean isPositionCached = false;
                if (world != null) {
                    if (world.isCached(dest)) {
                        isPositionCached = true;
                    }
                }
                long k = System.nanoTime();
                chunk2 += k - s;
                chunkCount2++;
                boolean currentlyLoaded = Minecraft.getMinecraft().world.getChunk(dest) instanceof EmptyChunk;
                long costStart = System.nanoTime();
                chunk += costStart - k;
                chunkCount++;
                if (!isPositionCached && currentlyLoaded) {
                    numEmptyChunk++;
                    continue;
                }

                // TODO cache cost
                double actionCost = movementToGetToNeighbor.getCost(calcContext);
                long costEnd = System.nanoTime();
                timeConsumed.put(movementToGetToNeighbor.getClass(), costEnd - costStart + timeConsumed.getOrDefault(movementToGetToNeighbor.getClass(), 0L));
                count.put(movementToGetToNeighbor.getClass(), 1 + count.getOrDefault(movementToGetToNeighbor.getClass(), 0));
                //System.out.println(movementToGetToNeighbor.getClass() + "" + (costEnd - costStart));
                numMovementsConsidered++;
                if (actionCost >= ActionCosts.COST_INF) {
                    continue;
                }
                if (actionCost <= 0) {
                    throw new IllegalStateException(movementToGetToNeighbor.getClass() + " " + movementToGetToNeighbor + " calculated implausible cost " + actionCost);
                }
                if (favoring && favored.contains(dest)) {
                    // see issue #18
                    actionCost *= favorCoeff;
                }
                long st = System.nanoTime();
                PathNode neighbor = getNodeAtPosition(dest);
                getNode += System.nanoTime() - st;
                getNodeCount++;
                double tentativeCost = currentNode.cost + actionCost;
                if (tentativeCost < neighbor.cost) {
                    if (tentativeCost < 0) {
                        throw new IllegalStateException(movementToGetToNeighbor.getClass() + " " + movementToGetToNeighbor + " overflowed into negative " + actionCost + " " + neighbor.cost + " " + tentativeCost);
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
                    neighbor.previousMovement = movementToGetToNeighbor;
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
                            bestHeuristicSoFar[i] = heuristic;
                            bestSoFar[i] = neighbor;
                        }
                    }
                }
            }
        }
        System.out.println("Remove " + (heapRemove / heapRemoveCount) + " " + heapRemove / 1000000 + " " + heapRemoveCount);
        System.out.println("Add " + (heapAdd / heapAddCount) + " " + heapAdd / 1000000 + " " + heapAddCount);
        System.out.println("Update " + (heapUpdate / heapUpdateCount) + " " + heapUpdate / 1000000 + " " + heapUpdateCount);
        System.out.println("Construction " + (construction / constructionCount) + " " + construction / 1000000 + " " + constructionCount);
        System.out.println("EmptyChunk " + (chunk / chunkCount) + " " + chunk / 1000000 + " " + chunkCount);
        System.out.println("CachedChunk " + (chunk2 / chunkCount2) + " " + chunk2 / 1000000 + " " + chunkCount2);
        System.out.println("GetNode " + (getNode / getNodeCount) + " " + getNode / 1000000 + " " + getNodeCount);
        ArrayList<Class<? extends Movement>> klasses = new ArrayList<>(count.keySet());
        klasses.sort(Comparator.comparingLong(k -> timeConsumed.get(k) / count.get(k)));
        for (Class<? extends Movement> klass : klasses) {
            int num = count.get(klass);
            long nanoTime = timeConsumed.get(klass);
            System.out.println(nanoTime / num + " " + klass + " " + nanoTime / 1000000 + "ms " + num);
        }
        if (cancelRequested) {
            currentlyRunning = null;
            return Optional.empty();
        }
        System.out.println(numMovementsConsidered + " movements considered");
        System.out.println("Open set size: " + ((BinaryHeapOpenSet) openSet).size());
        System.out.println((int) (numNodes * 1.0 / ((System.currentTimeMillis() - startTime) / 1000F)) + " nodes per second");
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
                displayChatMessageRaw("Took " + (System.currentTimeMillis() - startTime) + "ms, A* cost coefficient " + COEFFICIENTS[i]);
                if (COEFFICIENTS[i] >= 3) {
                    System.out.println("Warning: cost coefficient is greater than three! Probably means that");
                    System.out.println("the path I found is pretty terrible (like sneak-bridging for dozens of blocks)");
                    System.out.println("But I'm going to do it anyway, because yolo");
                }
                System.out.println("Path goes for " + Math.sqrt(dist) + " blocks");
                currentlyRunning = null;
                return Optional.of(new Path(startNode, bestSoFar[i], numNodes));
            }
        }
        displayChatMessageRaw("Even with a cost coefficient of " + COEFFICIENTS[COEFFICIENTS.length - 1] + ", I couldn't get more than " + bestDist + " blocks");
        displayChatMessageRaw("No path found =(");
        currentlyRunning = null;
        return Optional.empty();
    }


    public static Movement[] getConnectedPositions(BetterBlockPos pos, CalculationContext calcContext) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        BetterBlockPos east = new BetterBlockPos(x + 1, y, z);
        BetterBlockPos west = new BetterBlockPos(x - 1, y, z);
        BetterBlockPos south = new BetterBlockPos(x, y, z + 1);
        BetterBlockPos north = new BetterBlockPos(x, y, z - 1);
        return new Movement[]{
                new MovementTraverse(pos, east),
                new MovementTraverse(pos, west),
                new MovementTraverse(pos, north),
                new MovementTraverse(pos, south),
                new MovementAscend(pos, new BetterBlockPos(x + 1, y + 1, z)),
                new MovementAscend(pos, new BetterBlockPos(x - 1, y + 1, z)),
                new MovementAscend(pos, new BetterBlockPos(x, y + 1, z + 1)),
                new MovementAscend(pos, new BetterBlockPos(x, y + 1, z - 1)),
                MovementHelper.generateMovementFallOrDescend(pos, east, calcContext),
                MovementHelper.generateMovementFallOrDescend(pos, west, calcContext),
                MovementHelper.generateMovementFallOrDescend(pos, north, calcContext),
                MovementHelper.generateMovementFallOrDescend(pos, south, calcContext),
                new MovementDownward(pos, new BetterBlockPos(x, y - 1, z)),
                new MovementDiagonal(pos, EnumFacing.NORTH, EnumFacing.WEST),
                new MovementDiagonal(pos, EnumFacing.NORTH, EnumFacing.EAST),
                new MovementDiagonal(pos, EnumFacing.SOUTH, EnumFacing.WEST),
                new MovementDiagonal(pos, EnumFacing.SOUTH, EnumFacing.EAST),
                new MovementPillar(pos, new BetterBlockPos(x, y + 1, z))
        };
    }

    private final Random random = new Random();

    private <T> void shuffle(T[] list) {
        int len = list.length;
        for (int i = 0; i < len; i++) {
            int j = random.nextInt(len);
            T t = list[j];
            list[j] = list[i];
            list[i] = t;
        }
    }
}
