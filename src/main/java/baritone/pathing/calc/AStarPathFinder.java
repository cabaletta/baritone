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
import baritone.cache.CachedWorld;
import baritone.cache.WorldProvider;
import baritone.pathing.calc.openset.BinaryHeapOpenSet;
import baritone.pathing.goals.Goal;
import baritone.pathing.movement.ActionCosts;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.movements.*;
import baritone.pathing.path.IPath;
import baritone.utils.BlockStateInterface;
import baritone.utils.Helper;
import baritone.utils.pathing.BetterBlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;

/**
 * The actual A* pathfinding
 *
 * @author leijurv
 */
public class AStarPathFinder extends AbstractNodeCostSearch implements Helper {

    private final Optional<HashSet<BetterBlockPos>> favoredPositions;

    private final Random random = new Random();

    public AStarPathFinder(BlockPos start, Goal goal, Optional<Collection<BetterBlockPos>> favoredPositions) {
        super(start, goal);
        this.favoredPositions = favoredPositions.map(HashSet::new); // <-- okay this is epic
    }

    @Override
    protected Optional<IPath> calculate0(long timeout) {
        startNode = getNodeAtPosition(start);
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
        HashSet<BetterBlockPos> favored = favoredPositions.orElse(null);
        CachedWorld cachedWorld = Optional.ofNullable(WorldProvider.INSTANCE.getCurrentWorld()).map(w -> w.cache).orElse(null);
        ChunkProviderClient chunkProvider = Minecraft.getMinecraft().world.getChunkProvider();
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
            BetterBlockPos currentNodePos = currentNode.pos;
            numNodes++;
            if (goal.isInGoal(currentNodePos)) {
                logDebug("Took " + (System.nanoTime() / 1000000L - startTime) + "ms, " + numMovementsConsidered + " movements considered");
                return Optional.of(new Path(startNode, currentNode, numNodes, goal));
            }
            Movement[] possibleMovements = getConnectedPositions(currentNodePos, calcContext);//movement that we could take that start at currentNodePos, in random order
            shuffle(possibleMovements);
            for (Movement movementToGetToNeighbor : possibleMovements) {
                if (movementToGetToNeighbor == null) {
                    continue;
                }
                BetterBlockPos dest = movementToGetToNeighbor.getDest();
                int chunkX = currentNodePos.x >> 4;
                int chunkZ = currentNodePos.z >> 4;
                if (dest.x >> 4 != chunkX || dest.z >> 4 != chunkZ) {
                    // only need to check if the destination is a loaded chunk if it's in a different chunk than the start of the movement
                    if (chunkProvider.isChunkGeneratedAt(chunkX, chunkZ)) {
                        // see issue #106
                        if (cachedWorld == null || !cachedWorld.isCached(dest)) {
                            numEmptyChunk++;
                            continue;
                        }
                    }
                }
                // TODO cache cost
                double actionCost = movementToGetToNeighbor.getCost(calcContext);
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
                PathNode neighbor = getNodeAtPosition(dest);
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


    public static Movement[] getConnectedPositions(BetterBlockPos pos, CalculationContext calcContext) {
        int x = pos.x;
        int y = pos.y;
        int z = pos.z;
        BetterBlockPos east = new BetterBlockPos(x + 1, y, z);
        BetterBlockPos west = new BetterBlockPos(x - 1, y, z);
        BetterBlockPos south = new BetterBlockPos(x, y, z + 1);
        BetterBlockPos north = new BetterBlockPos(x, y, z - 1);
        return new Movement[]{
                new MovementDownward(pos, new BetterBlockPos(x, y - 1, z)),

                new MovementPillar(pos, new BetterBlockPos(x, y + 1, z)),

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

                new MovementDiagonal(pos, EnumFacing.NORTH, EnumFacing.EAST),
                new MovementDiagonal(pos, EnumFacing.NORTH, EnumFacing.WEST),
                new MovementDiagonal(pos, EnumFacing.SOUTH, EnumFacing.EAST),
                new MovementDiagonal(pos, EnumFacing.SOUTH, EnumFacing.WEST),

                MovementParkour.generate(pos, EnumFacing.EAST, calcContext),
                MovementParkour.generate(pos, EnumFacing.WEST, calcContext),
                MovementParkour.generate(pos, EnumFacing.NORTH, calcContext),
                MovementParkour.generate(pos, EnumFacing.SOUTH, calcContext),
        };
    }

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
