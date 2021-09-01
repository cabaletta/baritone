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

public class GreedySolver {

    private final SolverEngineInput engineInput;
    private final NodeBinaryHeap heap = new NodeBinaryHeap();
    private final Long2ObjectOpenHashMap<Node> nodes = new Long2ObjectOpenHashMap<>();
    final Long2ObjectOpenHashMap<WorldState> zobristWorldStateCache = new Long2ObjectOpenHashMap<>();
    private final Bounds bounds;

    public GreedySolver(SolverEngineInput input) {
        this.engineInput = input;
        this.bounds = engineInput.graph.bounds();
    }

    synchronized SolverEngineOutput search() {
        Node root = new Node(engineInput.player, 0L, -1L, this, 0);
        nodes.put(root.nodeMapKey(), root);
        heap.insert(root);
        zobristWorldStateCache.put(0L, new WorldState.WorldStateWrappedSubstrate(engineInput));
        while (!heap.isEmpty()) {
            expandNode(heap.removeLowest());
        }
        throw new UnsupportedOperationException();
    }

    private void expandNode(Node node) {
        WorldState worldState = node.coalesceState(this);
        long pos = node.pos();
        BlockStateCachedData aboveAbove = at(BetterBlockPos.offsetBy(pos, 0, 3, 0), worldState);
        BlockStateCachedData above = at(BetterBlockPos.offsetBy(pos, 0, 2, 0), worldState);
        BlockStateCachedData head = at(Face.UP.offset(pos), worldState);
        if (Main.DEBUG && head.collidesWithPlayer) { // needed because PlayerPhysics doesn't get this
            throw new IllegalStateException();
        }
        BlockStateCachedData feet = at(pos, worldState);
        BlockStateCachedData underneath = at(Face.DOWN.offset(pos), worldState);

        PlayerPhysics.VoxelResidency residency = PlayerPhysics.canPlayerStand(underneath, feet);
        if (!PlayerPhysics.valid(residency)) {
            throw new UnsupportedOperationException("sneaking off the edge of a block is not yet supported");
        }
        boolean standingWithinCollidableVoxel = residency == PlayerPhysics.VoxelResidency.STANDARD_WITHIN_SUPPORT;
        long playerIsActuallySupportedBy = standingWithinCollidableVoxel ? pos : Face.DOWN.offset(pos);
        int blipsWithinBlock = standingWithinCollidableVoxel ? feet.collisionHeightBlips() : underneath.collisionHeightBlips() - Blip.FULL_BLOCK;
        if (blipsWithinBlock < 0) {
            throw new IllegalStateException();
        }

        // pillar up
        {
            long maybePlaceAt = Face.UP.offset(playerIsActuallySupportedBy);
            if (!worldState.blockExists(maybePlaceAt)) {
                engineInput.graph.data(maybePlaceAt);
            } else {
                if (Main.DEBUG && at(maybePlaceAt, worldState).collidesWithPlayer) {
                    throw new IllegalStateException();
                }
            }
        }

        // walk sideways and either stay level, ascend, or descend
        for (Face travel : Face.HORIZONTALS) {
            long newPos = travel.offset(pos);
            switch (PlayerPhysics.playerTravelCollides(blipsWithinBlock, underneath, feet, above, aboveAbove, newPos, worldState, engineInput)) {
                case BLOCKED: {
                    continue;
                }
                case FALL: {
                    int descendBy = PlayerPhysics.playerFalls(newPos, worldState, engineInput);
                    if (descendBy != -1) {
                        if (Main.DEBUG && descendBy <= 0) {
                            throw new IllegalStateException();
                        }
                        upsertEdge(node, worldState, BetterBlockPos.offsetBy(newPos, 0, -descendBy, 0), -1, fallCost(descendBy));
                    }
                    break;
                }
                case VOXEL_LEVEL: {
                    upsertEdge(node, worldState, newPos, -1, flatCost());
                    break;
                }
                case JUMP_TO_VOXEL_LEVEL: {
                    upsertEdge(node, worldState, newPos, -1, jumpCost());
                    break;
                }
                case VOXEL_UP: {
                    upsertEdge(node, worldState, Face.UP.offset(newPos), -1, flatCost());
                    break;
                }
                case JUMP_TO_VOXEL_UP: {
                    upsertEdge(node, worldState, Face.UP.offset(newPos), -1, jumpCost());
                    break;
                }
                case JUMP_TO_VOXEL_TWO_UP: {
                    upsertEdge(node, worldState, BetterBlockPos.offsetBy(newPos, 0, 2, 0), -1, jumpCost());
                    break;
                }
                default:
                    throw new IllegalStateException();
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

    private void upsertEdge(Node node, WorldState worldState, long newPlayerPosition, long blockPlacement, int edgeCost) {
        if (Main.DEBUG && blockPlacement == -1 && PlayerPhysics.determinePlayerRealSupportLevel(at(Face.DOWN.offset(newPlayerPosition), worldState), at(newPlayerPosition, worldState)) < 0) {
            throw new IllegalStateException();
        }
        Node neighbor = getNode(newPlayerPosition, node, worldState, blockPlacement);
        if (Main.SLOW_DEBUG && blockPlacement != -1 && !neighbor.coalesceState(this).blockExists(blockPlacement)) { // only in slow_debug because this force-allocates a WorldState for every neighbor of every node!
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

    private Node getNode(long playerPosition, Node prev, WorldState prevWorld, long blockPlacement) {
        if (Main.DEBUG && blockPlacement != -1 && prev.coalesceState(this).blockExists(blockPlacement)) {
            throw new IllegalStateException();
        }
        long worldStateZobristHash = prev.worldStateZobristHash;
        if (blockPlacement != -1) {
            worldStateZobristHash = WorldState.updateZobrist(worldStateZobristHash, blockPlacement);
        }
        long code = playerPosition ^ worldStateZobristHash;
        Node existing = nodes.get(code);
        if (existing != null) {
            return existing;
        }
        int newHeuristic = prev.heuristic;
        if (blockPlacement != -1) {
            newHeuristic += calculateHeuristicModifier(prevWorld, blockPlacement);
        }
        Node node = new Node(playerPosition, worldStateZobristHash, blockPlacement, this, newHeuristic);
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
