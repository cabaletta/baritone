/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.pathfinding;

import baritone.pathfinding.goals.Goal;
import baritone.pathfinding.actions.ActionDescendTwo;
import baritone.pathfinding.actions.ActionBridge;
import baritone.pathfinding.actions.ActionClimb;
import baritone.pathfinding.actions.Action;
import baritone.pathfinding.actions.ActionDescend;
import baritone.pathfinding.actions.ActionFall;
import baritone.pathfinding.actions.ActionPillar;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import baritone.Baritone;
import baritone.pathfinding.actions.ActionDescendThree;
import baritone.pathfinding.actions.ActionWalkDiagonal;
import baritone.util.Out;
import baritone.util.ToolSet;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.chunk.EmptyChunk;

/**
 *
 * @author leijurv
 */
public class PathFinder {
    final BlockPos start;
    final Goal goal;
    final HashMap<BlockPos, Node> map;
    public PathFinder(BlockPos start, Goal goal) {
        this.start = start;
        this.goal = goal;
        this.map = new HashMap<BlockPos, Node>();
    }
    static final double[] COEFFICIENTS = {1.5, 2, 2.5, 3, 4, 5, 10};
    public static PathFinder currentlyRunning = null;
    Node[] bestSoFar;
    Node startNode;
    Node mostRecentConsidered;
    public Path getTempSolution() {
        if (startNode == null || bestSoFar[0] == null) {
            return null;
        }
        return new Path(startNode, bestSoFar[0], goal, 0);
    }
    public Path getMostRecentNodeConsidered() {
        return mostRecentConsidered == null ? null : new Path(startNode, mostRecentConsidered, goal, 0);
    }
    /**
     * Do the actual path calculation. The returned path might not actually go
     * to goal, but it will get as close as I could get
     *
     * @return
     */
    public Path calculatePath() {
        //a lot of these vars are local. that's because if someone tries to call this from multiple threads, they won't interfere (much)
        startNode = getNodeAtPosition(start);
        startNode.cost = 0;
        bestSoFar = new Node[COEFFICIENTS.length];//keep track of the best node by the metric of (estimatedCostToGoal + cost / COEFFICIENTS[i])
        double[] bestHeuristicSoFar = new double[COEFFICIENTS.length];
        for (int i = 0; i < bestHeuristicSoFar.length; i++) {
            bestHeuristicSoFar[i] = Double.MAX_VALUE;
        }
        OpenSet openSet = new OpenSet();
        startNode.isOpen = true;
        openSet.insert(startNode);
        currentlyRunning = this;
        long startTime = System.currentTimeMillis();
        long timeoutTime = startTime + (Baritone.slowPath ? 40000 : 4000);
        long lastPrintout = 0;
        int numNodes = 0;
        ToolSet ts = new ToolSet();
        int numEmptyChunk = 0;
        while (openSet.first != null && numEmptyChunk < 50 && System.currentTimeMillis() < timeoutTime) {
            if (Baritone.slowPath) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(PathFinder.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            Node currentNode = openSet.removeLowest();
            mostRecentConsidered = currentNode;
            currentNode.isOpen = false;
            currentNode.nextOpen = null;
            BlockPos currentNodePos = currentNode.pos;
            numNodes++;
            if (System.currentTimeMillis() > lastPrintout + 1000) {//print once a second
                Out.log("searching... at " + currentNodePos + ", considered " + numNodes + " nodes so far");
                lastPrintout = System.currentTimeMillis();
            }
            if (goal.isInGoal(currentNodePos)) {
                currentlyRunning = null;
                return new Path(startNode, currentNode, goal, numNodes);
            }
            //long constructStart = System.nanoTime();
            Action[] possibleActions = getConnectedPositions(currentNodePos);//actions that we could take that start at myPos, in random order
            shuffle(possibleActions);
            //long constructEnd = System.nanoTime();
            //System.out.println(constructEnd - constructStart);
            for (Action actionToGetToNeighbor : possibleActions) {
                //long costStart = System.nanoTime();
                double actionCost = actionToGetToNeighbor.cost(ts);
                //long costEnd = System.nanoTime();
                //System.out.println(actionToGetToNeighbor.getClass() + "" + (costEnd - costStart));
                if (actionCost >= Action.COST_INF) {
                    continue;
                }
                if (Minecraft.getMinecraft().world.getChunkFromBlockCoords(actionToGetToNeighbor.to) instanceof EmptyChunk) {
                    numEmptyChunk++;
                    continue;
                }
                Node neighbor = getNodeAtPosition(actionToGetToNeighbor.to);
                double tentativeCost = currentNode.cost + actionCost;
                if (tentativeCost < neighbor.cost) {
                    neighbor.previous = currentNode;
                    neighbor.previousAction = actionToGetToNeighbor;
                    neighbor.cost = tentativeCost;
                    if (!neighbor.isOpen) {
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
            double dist = distFromStart(bestSoFar[i]);
            if (dist > bestDist) {
                bestDist = dist;
            }
            if (dist > MIN_DIST_PATH) {
                Out.gui("A* cost coefficient " + COEFFICIENTS[i], Out.Mode.Debug);
                if (COEFFICIENTS[i] >= 3) {
                    Out.gui("Warning: cost coefficient is greater than three! Probably means that", Out.Mode.Debug);
                    Out.gui("the path I found is pretty terrible (like sneak-bridging for dozens of blocks)", Out.Mode.Debug);
                    Out.gui("But I'm going to do it anyway, because yolo", Out.Mode.Debug);
                }
                Out.gui("Path goes for " + dist + " blocks", Out.Mode.Debug);
                currentlyRunning = null;
                return new Path(startNode, bestSoFar[i], goal, numNodes);
            }
        }
        Out.gui("Even with a cost coefficient of " + COEFFICIENTS[COEFFICIENTS.length - 1] + ", I couldn't get more than " + bestDist + " blocks =(", Out.Mode.Debug);
        Out.gui("No path found =(", Out.Mode.Standard);
        currentlyRunning = null;
        return null;
    }
    private double distFromStart(Node n) {
        int xDiff = n.pos.getX() - start.getX();
        int yDiff = n.pos.getY() - start.getY();
        int zDiff = n.pos.getZ() - start.getZ();
        return Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
    }
    private final double MIN_DIST_PATH = 5;
    private Node getNodeAtPosition(BlockPos pos) {
        Node alr = map.get(pos);
        if (alr == null) {
            Node node = new Node(pos, goal);
            map.put(pos, node);
            return node;
        }
        return alr;
    }
    private static Action[] getConnectedPositions(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        Action[] actions = new Action[26];
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
        return actions;
    }
    private final Random random = new Random();
    private void shuffle(Action[] list) {
        int len = list.length;
        for (int i = 0; i < len; i++) {
            int j = random.nextInt(len);
            Action e = list[j];
            list[j] = list[i];
            list[i] = e;
        }
    }

    /**
     * My own implementation of a singly linked list
     */
    private static class OpenSet {
        Node first = null;
        public Node removeLowest() {
            if (first == null) {
                return null;
            }
            Node current = first.nextOpen;
            if (current == null) {
                Node n = first;
                first = null;
                return n;
            }
            Node previous = first;
            double bestValue = first.estimatedCostToGoal + first.cost;
            Node bestNode = first;
            Node beforeBest = null;
            while (current != null) {
                double comp = current.estimatedCostToGoal + current.cost;
                if (comp < bestValue) {
                    bestValue = comp;
                    bestNode = current;
                    beforeBest = previous;
                }
                previous = current;
                current = current.nextOpen;
            }
            if (beforeBest == null) {
                first = first.nextOpen;
                return bestNode;
            }
            beforeBest.nextOpen = bestNode.nextOpen;
            return bestNode;
        }
        public void insert(Node node) {
            node.nextOpen = first;
            first = node;
        }
    }
}
