package baritone.bot.pathing.calc;

import baritone.bot.pathing.calc.openset.BinaryHeapOpenSet;
import baritone.bot.pathing.calc.openset.IOpenSet;
import baritone.bot.pathing.goals.Goal;
import baritone.bot.pathing.movement.ActionCosts;
import baritone.bot.pathing.movement.Movement;
import baritone.bot.pathing.movement.movements.MovementAscend;
import baritone.bot.pathing.movement.movements.MovementTraverse;
import baritone.bot.pathing.path.IPath;
import baritone.bot.utils.ToolSet;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.EmptyChunk;

import java.util.Optional;
import java.util.Random;

/**
 * The actual A* pathfinding
 *
 * @author leijurv
 */
public class AStarPathFinder extends AbstractNodeCostSearch {

    public static boolean slowPath = false;

    public AStarPathFinder(BlockPos start, Goal goal) {
        super(start, goal);
    }

    @Override
    protected Optional<IPath> calculate0() {
        startNode = getNodeAtPosition(start);
        startNode.cost = 0;
        startNode.combinedCost = startNode.estimatedCostToGoal;
        IOpenSet openSet = new BinaryHeapOpenSet();
        startNode.isOpen = true;
        openSet.insert(startNode);
        bestSoFar = new PathNode[COEFFICIENTS.length];//keep track of the best node by the metric of (estimatedCostToGoal + cost / COEFFICIENTS[i])
        double[] bestHeuristicSoFar = new double[COEFFICIENTS.length];
        for (int i = 0; i < bestHeuristicSoFar.length; i++) {
            bestHeuristicSoFar[i] = Double.MAX_VALUE;
        }
        currentlyRunning = this;
        long startTime = System.currentTimeMillis();
        long timeoutTime = startTime + (slowPath ? 40000 : 4000);
        long lastPrintout = 0;
        int numNodes = 0;
        ToolSet ts = new ToolSet();
        int numEmptyChunk = 0;
        while (!openSet.isEmpty() && numEmptyChunk < 50 && System.currentTimeMillis() < timeoutTime) {
            if (slowPath) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }
            }
            PathNode currentNode = openSet.removeLowest();
            currentNode.isOpen = false;
            mostRecentConsidered = currentNode;
            BlockPos currentNodePos = currentNode.pos;
            numNodes++;
            if (System.currentTimeMillis() > lastPrintout + 1000) {//print once a second
                System.out.println("searching... at " + currentNodePos + ", considered " + numNodes + " nodes so far");
                lastPrintout = System.currentTimeMillis();
            }
            if (goal.isInGoal(currentNodePos)) {
                currentlyRunning = null;
                return Optional.of(new Path(startNode, currentNode, goal, numNodes));
            }
            //long constructStart = System.nanoTime();
            Movement[] possibleMovements = getConnectedPositions(currentNodePos);//movement that we could take that start at myPos, in random order
            shuffle(possibleMovements);
            //long constructEnd = System.nanoTime();
            //System.out.println(constructEnd - constructStart);
            for (Movement movementToGetToNeighbor : possibleMovements) {
                if (Minecraft.getMinecraft().world.getChunk(movementToGetToNeighbor.getDest()) instanceof EmptyChunk) {
                    numEmptyChunk++;
                    continue;
                }
                //long costStart = System.nanoTime();
                // TODO cache cost
                double actionCost = movementToGetToNeighbor.getCost(ts);
                //long costEnd = System.nanoTime();
                //System.out.println(movementToGetToNeighbor.getClass() + "" + (costEnd - costStart));
                if (actionCost >= ActionCosts.COST_INF) {
                    continue;
                }
                PathNode neighbor = getNodeAtPosition(movementToGetToNeighbor.getDest());
                double tentativeCost = currentNode.cost + actionCost;
                if (tentativeCost < neighbor.cost) {
                    neighbor.previous = currentNode;
                    neighbor.previousMovement = movementToGetToNeighbor;
                    neighbor.cost = tentativeCost;
                    neighbor.combinedCost = tentativeCost + neighbor.estimatedCostToGoal;
                    if (neighbor.isOpen) {
                        openSet.update(neighbor);
                    } else {
                        openSet.insert(neighbor);//dont double count, dont insert into open set if it's already there
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
                System.out.println("A* cost coefficient " + COEFFICIENTS[i]);
                if (COEFFICIENTS[i] >= 3) {
                    System.out.println("Warning: cost coefficient is greater than three! Probably means that");
                    System.out.println("the path I found is pretty terrible (like sneak-bridging for dozens of blocks)");
                    System.out.println("But I'm going to do it anyway, because yolo");
                }
                System.out.println("Path goes for " + dist + " blocks");
                currentlyRunning = null;
                return Optional.of(new Path(startNode, bestSoFar[i], goal, numNodes));
            }
        }
        System.out.println("Even with a cost coefficient of " + COEFFICIENTS[COEFFICIENTS.length - 1] + ", I couldn't get more than " + bestDist + " blocks =(");
        System.out.println("No path found =(");
        currentlyRunning = null;
        return Optional.empty();
    }


    private static Movement[] getConnectedPositions(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        Movement[] movements = new Movement[8];
        movements[0] = new MovementTraverse(pos, new BlockPos(x + 1, y, z));
        movements[1] = new MovementTraverse(pos, new BlockPos(x - 1, y, z));
        movements[2] = new MovementTraverse(pos, new BlockPos(x, y, z + 1));
        movements[3] = new MovementTraverse(pos, new BlockPos(x, y, z - 1));
        movements[4] = new MovementAscend(pos, new BlockPos(x + 1, y + 1, z));
        movements[5] = new MovementAscend(pos, new BlockPos(x - 1, y + 1, z));
        movements[6] = new MovementAscend(pos, new BlockPos(x, y + 1, z + 1));
        movements[7] = new MovementAscend(pos, new BlockPos(x, y + 1, z - 1));
        /*Action[] actions = new Action[26];
        actions[0] = new ActionPillar(pos);
        actions[1] = new ActionBridge(pos, new BlockPos(x + 1, y, z));
        actions[2] = new ActionBridge(pos, new BlockPos(x - 1, y, z));
        actions[3] = new ActionBridge(pos, new BlockPos(x, y, z + 1));
        actions[4] = new ActionBridge(pos, new BlockPos(x, y, z - 1));
        actions[5] = new ActionClimb(pos, new BlockPos(x + 1, y + 1, z));
        actions[6] = new ActionClimb(pos, new BlockPos(x - 1, y + 1, z));
        actions[7] = new ActionClimb(pos, new BlockPos(x, y + 1, z + 1));
        actions[8] = new ActionClimb(pos, new BlockPos(x, y + 1, z - 1));
        actions[9] = new ActionDescend(pos, new BlockPos(x, y - 1, z - 1));
        actions[10] = new ActionDescend(pos, new BlockPos(x, y - 1, z + 1));
        actions[11] = new ActionDescend(pos, new BlockPos(x - 1, y - 1, z));
        actions[12] = new ActionDescend(pos, new BlockPos(x + 1, y - 1, z));
        actions[13] = new ActionFall(pos);
        actions[14] = new ActionDescendTwo(pos, new BlockPos(x, y - 2, z - 1));
        actions[15] = new ActionDescendTwo(pos, new BlockPos(x, y - 2, z + 1));
        actions[16] = new ActionDescendTwo(pos, new BlockPos(x - 1, y - 2, z));
        actions[17] = new ActionDescendTwo(pos, new BlockPos(x + 1, y - 2, z));
        actions[18] = new ActionDescendThree(pos, new BlockPos(x, y - 3, z - 1));
        actions[19] = new ActionDescendThree(pos, new BlockPos(x, y - 3, z + 1));
        actions[20] = new ActionDescendThree(pos, new BlockPos(x - 1, y - 3, z));
        actions[21] = new ActionDescendThree(pos, new BlockPos(x + 1, y - 3, z));
        actions[22] = new ActionWalkDiagonal(pos, EnumFacing.NORTH, EnumFacing.WEST);
        actions[23] = new ActionWalkDiagonal(pos, EnumFacing.NORTH, EnumFacing.EAST);
        actions[24] = new ActionWalkDiagonal(pos, EnumFacing.SOUTH, EnumFacing.WEST);
        actions[25] = new ActionWalkDiagonal(pos, EnumFacing.SOUTH, EnumFacing.EAST);
        return actions;*/
        return movements;
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
