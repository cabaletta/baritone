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

    public GreedySolver(SolverEngineInput input) {
        this.engineInput = input;
    }

    NodeBinaryHeap heap = new NodeBinaryHeap();
    Long2ObjectOpenHashMap<Node> nodes = new Long2ObjectOpenHashMap<>();
    Long2ObjectOpenHashMap<WorldState> zobristWorldStateCache = new Long2ObjectOpenHashMap<>();

    Node getNode(long playerPosition, long worldStateZobristHash, long blockPlacement) {
        long code = playerPosition ^ worldStateZobristHash;
        Node existing = nodes.get(code);
        if (existing != null) {
            return existing;
        }
        Node node = new Node(BetterBlockPos.XfromLong(playerPosition), BetterBlockPos.YfromLong(playerPosition), BetterBlockPos.ZfromLong(playerPosition), worldStateZobristHash, blockPlacement, this);
        if (Main.DEBUG && node.nodeMapKey() != code) {
            throw new IllegalStateException();
        }
        nodes.put(code, node);
        return node;
    }

    SolverEngineOutput search() {
        while (!heap.isEmpty()) {
            Node node = heap.removeLowest();
            WorldState worldState = node.coalesceState(this);

            // consider actions:
            // traverse
            // pillar
            // place blocks beneath feet
        }
        throw new UnsupportedOperationException();
    }

    public BlockStateCachedData at(long pos, WorldState inWorldState) {
        return inWorldState.blockExists(pos) ? engineInput.graph.data(pos) : BlockStateCachedData.AIR;
    }
}
