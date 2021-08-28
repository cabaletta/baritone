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

    SolverEngineInput engineInput;
    NodeBinaryHeap heap = new NodeBinaryHeap();
    Long2ObjectOpenHashMap<Node> nodes = new Long2ObjectOpenHashMap<>();
    Long2ObjectOpenHashMap<WorldState> zobristWorldStateCache = new Long2ObjectOpenHashMap<>();

    public GreedySolver(SolverEngineInput input) {
        this.engineInput = input;
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
        long pos = node.pos;
        BlockStateCachedData aboveAbove = at(BetterBlockPos.offsetBy(pos, 0, 3, 0), worldState);
        BlockStateCachedData above = at(BetterBlockPos.offsetBy(pos, 0, 2, 0), worldState);
        BlockStateCachedData head = at(Face.UP.offset(pos), worldState);
        if (Main.DEBUG && head.collidesWithPlayer) { // needed because PlayerPhysics doesn't get this
            throw new IllegalStateException();
        }
        BlockStateCachedData feet = at(pos, worldState);
        BlockStateCachedData underneath = at(Face.DOWN.offset(pos), worldState);
        int blipsWithinBlock = PlayerPhysics.determinePlayerRealSupportLevel(underneath, feet);
        if (blipsWithinBlock < 0) {
            throw new IllegalStateException();
        }

        cardinals:
        for (Face travel : Face.HORIZONTALS) {
            long newPos = travel.offset(pos);
            BlockStateCachedData newAboveAbove = at(BetterBlockPos.offsetBy(newPos, 0, 3, 0), worldState);
            BlockStateCachedData newAbove = at(BetterBlockPos.offsetBy(newPos, 0, 2, 0), worldState);
            BlockStateCachedData newHead = at(Face.UP.offset(newPos), worldState);
            BlockStateCachedData newFeet = at(newPos, worldState);
            BlockStateCachedData newUnderneath = at(Face.DOWN.offset(newPos), worldState);
            switch (PlayerPhysics.playerTravelCollides(blipsWithinBlock, above, newAbove, newHead, newFeet, newUnderneath, underneath, feet, aboveAbove, newAboveAbove)) {
                case BLOCKED: {
                    continue;
                }
                case FALL: {
                    // this means that there is nothing preventing us from walking forward and falling
                    // iterate downwards to see what we would hit
                    for (int descent = 0; ; descent++) {
                        // NOTE: you cannot do (descent*Face.DOWN.offset)&BetterBlockPos.POST_ADDITION_MASK because Y is serialized into the center of the long. but I suppose you could do it with X. hm maybe Y should be moved to the most significant bits purely to allow this :^)
                        long support = BetterBlockPos.offsetBy(newPos, 0, -descent, 0);
                        long under = Face.DOWN.offset(support);
                        if (Main.DEBUG && !engineInput.graph.bounds().inRangePos(under)) {
                            throw new IllegalStateException(); // should be caught by PREVENTED_BY_UNDERNEATH
                        }
                        PlayerPhysics.VoxelResidency res = PlayerPhysics.canPlayerStand(at(under, worldState), at(support, worldState));
                        if (Main.DEBUG && descent == 0 && res != PlayerPhysics.VoxelResidency.FLOATING) {
                            throw new IllegalStateException(); // CD shouldn't collide, it should be D and the one beneath...
                        }
                        switch (res) {
                            case FLOATING:
                                continue; // as expected
                            case PREVENTED_BY_UNDERNEATH:
                            case PREVENTED_BY_WITHIN:
                                continue cardinals; // no safe landing
                            case IMPOSSIBLE_WITHOUT_SUFFOCATING:
                                throw new IllegalStateException();
                            case UNDERNEATH_PROTRUDES_AT_OR_ABOVE_FULL_BLOCK:
                            case STANDARD_WITHIN_SUPPORT:
                                // found our landing spot
                                upsertEdge(node, worldState, support, -1, fallCost(descent));
                            default:
                                throw new IllegalStateException();
                        }
                    }
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
        return inWorldState.blockExists(pos) ? engineInput.graph.data(pos) : BlockStateCachedData.AIR;
    }
}
