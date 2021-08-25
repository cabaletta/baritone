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
        BlockStateCachedData above = at(BetterBlockPos.offsetBy(pos, 0, 2, 0), worldState);
        BlockStateCachedData head = at(Face.UP.offset(pos), worldState);
        if (Main.DEBUG && head.collidesWithPlayer) {
            throw new IllegalStateException();
        }
        BlockStateCachedData feet = at(pos, worldState);
        BlockStateCachedData underneath = at(Face.DOWN.offset(pos), worldState);
        int blipsWithinBlock = PlayerPhysics.determinePlayerRealSupportLevel(underneath, feet);
        if (blipsWithinBlock < 0) {
            throw new IllegalStateException();
        }
        boolean stickingUpIntoThirdBlock = blipsWithinBlock > Blip.TWO_BLOCKS - Blip.PLAYER_HEIGHT_SLIGHT_OVERESTIMATE; // exactly equal means not sticking up, since overestimate means overestimate
        int blips = BetterBlockPos.YfromLong(pos) * Blip.PER_BLOCK + blipsWithinBlock;

        mid:
        for (Face travel : Face.HORIZONTALS) {
            long newPos = travel.offset(pos);
            BlockStateCachedData newAbove = at(BetterBlockPos.offsetBy(newPos, 0, 2, 0), worldState);
            BlockStateCachedData newHead = at(Face.UP.offset(newPos), worldState);
            BlockStateCachedData newFeet = at(newPos, worldState);
            BlockStateCachedData newUnderneath = at(Face.DOWN.offset(newPos), worldState);
            switch (PlayerPhysics.playerTravelCollides(blipsWithinBlock, above, newAbove, newHead, newFeet, newUnderneath, underneath, feet)) {
                case BLOCKED: {
                    // TODO ascend
                    continue;
                }
                case FALL: {
                    // this means that there is nothing preventing us from walking forward and falling
                    // iterate downwards to see what we would hit
                    for (int descent = 0; ; descent++) {
                        // NOTE: you cannot do (descent*Face.DOWN.offset)&BetterBlockPos.POST_ADDITION_MASK because Y is serialized into the center of the long. but I suppose you could do it with X. hm maybe Y should be moved to the most significant bits purely to allow this :^)
                        long support = BetterBlockPos.offsetBy(newPos, 0, -descent, 0);
                        BlockStateCachedData data = at(support, worldState);
                        BlockStateCachedData under = at(Face.DOWN.offset(support), worldState);
                        PlayerPhysics.VoxelResidency res = PlayerPhysics.canPlayerStand(under, data);
                        switch (res) {
                            case FLOATING:
                                continue; // as expected
                            case PREVENTED_BY_UNDERNEATH:
                            case PREVENTED_BY_WITHIN:
                                continue mid; // no safe landing
                            case IMPOSSIBLE_WITHOUT_SUFFOCATING:
                                throw new IllegalStateException();
                            case UNDERNEATH_PROTRUDES_AT_OR_ABOVE_FULL_BLOCK:
                            case STANDARD_WITHIN_SUPPORT:
                                // found our landing spot
                                // TODO
                            default:
                                throw new IllegalStateException();
                        }
                    }
                    // descent, with a possible fallthrough to VOXEL_LEVEL if it's just a troll

                    // no break, fallthrouh instead!
                } // FALLTHROUGH!
                case VOXEL_LEVEL: { // ^ POSSIBLE FALLTHROUGH ^

                }
                case VOXEL_UP: {

                }
            }
        }

        // consider actions:
        // traverse
        // pillar
        // place blocks beneath feet
    }

    private void updateNeighbor(Node node, WorldState worldState, long newPlayerPosition, long blockPlacement, int edgeCost) {
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
