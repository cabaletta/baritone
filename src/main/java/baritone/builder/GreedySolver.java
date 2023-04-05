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

package baritone.builder;

import baritone.api.utils.BetterBlockPos;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class GreedySolver {

    private final SolverEngineInput engineInput;
    private final NodeBinaryHeap heap = new NodeBinaryHeap();
    private final Long2ObjectOpenHashMap<Node> nodes = new Long2ObjectOpenHashMap<>();
    private final ZobristWorldStateCache zobristCache;
    private final long allCompleted;
    private final Bounds bounds;
    private Column scratchpadExpandNode1 = new Column();
    private Column scratchpadExpandNode2 = new Column();
    private Column scratchpadExpandNode3 = new Column();

    public GreedySolver(SolverEngineInput input) {
        this.engineInput = input;
        this.bounds = engineInput.graph.bounds();
        this.zobristCache = new ZobristWorldStateCache(new WorldState.WorldStateWrappedSubstrate(engineInput));
        Node root = new Node(engineInput.player, null, 0L, -1L, 0);
        nodes.put(root.nodeMapKey(), root);
        heap.insert(root);
        this.allCompleted = WorldState.predetermineGoalZobrist(engineInput.allToPlaceNow);
    }

    synchronized SolverEngineOutput search() {
        while (!heap.isEmpty()) {
            Node node = heap.removeLowest();
            if (!node.sneaking() && node.worldStateZobristHash == allCompleted) {
                return backwards(node);
            }
            expandNode(node);
        }
        throw new UnsupportedOperationException();
    }

    private SolverEngineOutput backwards(Node node) {
        ArrayDeque<SolvedActionStep> steps = new ArrayDeque<>();
        while (node.previous != null) {
            steps.addFirst(step(node, node.previous));
            node = node.previous;
        }
        return new SolverEngineOutput(new ArrayList<>(steps));
    }

    private SolvedActionStep step(Node next, Node prev) {
        if (next.worldStateZobristHash == prev.worldStateZobristHash) {
            return new SolvedActionStep(next.pos());
        } else {
            return new SolvedActionStep(next.pos(), WorldState.unzobrist(prev.worldStateZobristHash ^ next.worldStateZobristHash));
        }
    }

    private boolean wantToPlaceAt(long blockGoesAt, Node vantage, int blipsWithinVoxel, WorldState worldState) {
        if (worldState.blockExists(blockGoesAt)) {
            return false;
        }
        long vpos = vantage.pos();
        int relativeX = BetterBlockPos.XfromLong(vpos) - BetterBlockPos.XfromLong(blockGoesAt);
        int relativeY = blipsWithinVoxel + Blip.FULL_BLOCK * (BetterBlockPos.YfromLong(vpos) - BetterBlockPos.YfromLong(blockGoesAt));
        int relativeZ = BetterBlockPos.ZfromLong(vpos) - BetterBlockPos.ZfromLong(blockGoesAt);
        BlockStateCachedData blockBeingPlaced = engineInput.graph.data(blockGoesAt);
        for (BlockStatePlacementOption option : blockBeingPlaced.placeMe) {
            long maybePlaceAgainst = option.against.offset(blockGoesAt);
            if (!bounds.inRangePos(maybePlaceAgainst)) {
                continue;
            }
            if (!worldState.blockExists(maybePlaceAgainst)) {
                continue;
            }
            BlockStateCachedData placingAgainst = engineInput.graph.data(maybePlaceAgainst);
            PlaceAgainstData againstData = placingAgainst.againstMe(option);
            traces:
            for (Raytracer.Raytrace trace : option.computeTraceOptions(againstData, relativeX, relativeY, relativeZ, PlayerVantage.LOOSE_CENTER, blockReachDistance())) { // TODO or only take the best one
                for (long l : trace.passedThrough) {
                    if (worldState.blockExists(l)) {
                        continue traces;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private void expandNode(Node node) {
        WorldState worldState = zobristCache.coalesceState(node);
        long pos = node.pos();
        Column within = scratchpadExpandNode1;
        within.initFrom(pos, worldState, engineInput);

        Column supportedBy;
        boolean sneaking = node.sneaking();
        if (sneaking) {
            supportedBy = scratchpadExpandNode3;
            long supportedFeetVoxel = SneakPosition.sneakDirectionFromPlayerToSupportingBlock(node.sneakingPosition()).offset(pos);
            supportedBy.initFrom(supportedFeetVoxel, worldState, engineInput);
            if (Main.DEBUG && !within.okToSneakIntoHereAtHeight(supportedBy.feetBlips)) {
                throw new IllegalStateException();
            }
        } else {
            supportedBy = within;
        }

        int playerFeet = supportedBy.feetBlips;
        if (Main.DEBUG && !supportedBy.playerCanExistAtFootBlip(playerFeet)) {
            throw new IllegalStateException();
        }

        // -------------------------------------------------------------------------------------------------------------
        // place block beneath or within feet voxel
        for (int dy = -1; dy <= 0; dy++) {
            // this is the common case for sneak bridging with full blocks
            long maybePlaceAt = BetterBlockPos.offsetBy(pos, 0, dy, 0);
            BlockStateCachedData wouldBePlaced = engineInput.graph.data(maybePlaceAt);
            int cost = blockPlaceCost();
            int playerFeetWouldBeAt = playerFeet;
            /*if (wouldBePlaced.collidesWithPlayer) {
                int heightRelativeToCurrentVoxel = wouldBePlaced.collisionHeightBlips() + dy * Blip.FULL_BLOCK;
                if (heightRelativeToCurrentVoxel > playerFeet) {
                    // we would need to jump in order to do this
                    cost += jumpCost();
                    playerFeetWouldBeAt = heightRelativeToCurrentVoxel; // because we'd have to jump, and could only place the block once we had cleared the collision box for it
                    if (!within.playerCanExistAtFootBlip(heightRelativeToCurrentVoxel) || !supportedBy.playerCanExistAtFootBlip(heightRelativeToCurrentVoxel)) {
                        continue;
                    }
                }
            }
            if (wantToPlaceAt(maybePlaceAt, node, playerFeetWouldBeAt, worldState)) {
                upsertEdge(node, worldState, pos, null, maybePlaceAt, cost);
            }*/
        }

        // -------------------------------------------------------------------------------------------------------------
        if (sneaking) {
            // we can walk back to where we were
            upsertEdge(node, worldState, supportedBy.pos, null, -1, flatCost());
            // this will probably rarely be used. i can only imagine rare scenarios such as needing the extra perspective in order to place a block a bit more efficiently. like, this could avoid unnecessary ancillary scaffolding i suppose.
            // ----
            // also let's try just letting ourselves fall off the edge of the block
            int descendBy = PlayerPhysics.playerFalls(pos, worldState, engineInput);
            if (descendBy != -1) {
                upsertEdge(node, worldState, BetterBlockPos.offsetBy(pos, 0, -descendBy, 0), null, -1, fallCost(descendBy));
            }
            return;
        }
        // not sneaking! sneaking returned ^^
        // -------------------------------------------------------------------------------------------------------------
        // walk sideways and either stay level, ascend, or descend
        Column into = scratchpadExpandNode2;
        for (Face travel : Face.HORIZONTALS) {
            long newPos = travel.offset(pos);
            into.initFrom(newPos, worldState, engineInput);
            PlayerPhysics.Collision collision = PlayerPhysics.playerTravelCollides(within, into);
            switch (collision) {
                case BLOCKED: {
                    continue;
                }
                case FALL: {
                    upsertEdge(node, worldState, newPos, travel, -1, flatCost()); // sneak off edge of block
                    break;
                }
                default: {
                    long realNewPos = BetterBlockPos.offsetBy(newPos, 0, collision.voxelVerticalOffset(), 0);
                    upsertEdge(node, worldState, realNewPos, null, -1, collision.requiresJump() ? jumpCost() : flatCost());
                    break;
                }
            }
        }
    }

    private int fallCost(int blocks) {
        if (blocks < 1) {
            throw new IllegalStateException();
        }
        throw new UnsupportedOperationException();
    }

    private int flatCost() {
        throw new UnsupportedOperationException();
    }

    private int jumpCost() {
        throw new UnsupportedOperationException();
    }

    private double blockReachDistance() {
        throw new UnsupportedOperationException();
    }

    private void upsertEdge(Node node, WorldState worldState, long newPlayerPosition, Face sneakingTowards, long blockPlacement, int edgeCost) {
        Node neighbor = getNode(newPlayerPosition, sneakingTowards, node, worldState, blockPlacement);
        if (Main.SLOW_DEBUG && blockPlacement != -1 && !zobristCache.coalesceState(neighbor).blockExists(blockPlacement)) { // only in slow_debug because this force-allocates a WorldState for every neighbor of every node!
            throw new IllegalStateException();
        }
        if (Main.DEBUG && node == neighbor) {
            throw new IllegalStateException();
        }
        updateNeighbor(node, neighbor, edgeCost);
    }

    private void updateNeighbor(Node node, Node neighbor, int edgeCost) {
        int currentCost = neighbor.cost;
        int offeredCost = node.cost + edgeCost;
        if (currentCost < offeredCost) {
            return;
        }
        neighbor.previous = node;
        neighbor.cost = offeredCost;
        neighbor.combinedCost = offeredCost + neighbor.heuristic;
        if (Main.DEBUG && neighbor.combinedCost < Integer.MIN_VALUE / 2) { // simple attempt to catch obvious overflow
            throw new IllegalStateException();
        }
        if (neighbor.inHeap()) {
            heap.update(neighbor);
        } else {
            heap.insert(neighbor);
        }
    }

    private int calculateHeuristicModifier(WorldState previous, long blockPlacedAt) {
        if (Main.DEBUG && previous.blockExists(blockPlacedAt)) {
            throw new IllegalStateException();
        }
        if (true) {
            throw new UnsupportedOperationException("tune the values first lol");
        }
        switch (engineInput.desiredToBePlaced(blockPlacedAt)) {
            case PART_OF_CURRENT_GOAL:
            case SCAFFOLDING_OF_CURRENT_GOAL:
                return -100; // keep kitten on task
            case PART_OF_FUTURE_GOAL:
                return -10; // smaller kitten treat for working ahead
            case SCAFFOLDING_OF_FUTURE_GOAL:
                return -5; // smallest kitten treat for working ahead on scaffolding
            case ANCILLARY:
                return 0; // no kitten treat for placing a random extra block
            default:
                throw new IllegalStateException();
        }
    }

    private int blockPlaceCost() {
        // maybe like... ten?
        throw new UnsupportedOperationException();
    }

    private Node getNode(long playerPosition, Face sneakingTowards, Node prev, WorldState prevWorld, long blockPlacement) {
        if (Main.DEBUG && blockPlacement != -1 && prevWorld.blockExists(blockPlacement)) {
            throw new IllegalStateException();
        }
        long worldStateZobristHash = prev.worldStateZobristHash;
        if (blockPlacement != -1) {
            worldStateZobristHash = WorldState.updateZobrist(worldStateZobristHash, blockPlacement);
        }
        long code = SneakPosition.encode(playerPosition, sneakingTowards) ^ worldStateZobristHash;
        Node existing = nodes.get(code);
        if (existing != null) {
            return existing;
        }
        int newHeuristic = prev.heuristic;
        if (blockPlacement != -1) {
            newHeuristic += calculateHeuristicModifier(prevWorld, blockPlacement);
        }
        Node node = new Node(playerPosition, null, worldStateZobristHash, blockPlacement, newHeuristic);
        if (Main.DEBUG && node.nodeMapKey() != code) {
            throw new IllegalStateException();
        }
        nodes.put(code, node);
        return node;
    }

    public BlockStateCachedData at(long pos, WorldState inWorldState) {
        return engineInput.at(pos, inWorldState);
    }
}
