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

public class Node {

    public int x;
    public int y;
    public int z;

    public long worldStateZobristHash;

    public int heuristic;
    public int cost;
    public int combinedCost;
    public Node previous;
    public int heapPosition;

    // boolean unrealizedZobristBlockChange; // no longer needed since presence in the overall GreedySolver zobristWorldStateCache indicates if this is a yet-unrealized branch of the zobrist space
    long packedUnrealizedCoordinate;
    // int unrealizedState; // no longer needed now that world state is binarized with scaffolding/build versus air
    // long unrealizedZobristParentHash; // no longer needed since we can compute it backwards with XOR

    public Node(int x, int y, int z, long zobristState, long unrealizedBlockPlacement, GreedySolver solver, int heuristic) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.heapPosition = -1;
        this.cost = Integer.MAX_VALUE;
        this.heuristic = heuristic;
        this.worldStateZobristHash = zobristState;
        this.packedUnrealizedCoordinate = unrealizedBlockPlacement;
        if (Main.DEBUG && (solver.zobristWorldStateCache.containsKey(worldStateZobristHash) ^ (unrealizedBlockPlacement == -1))) {
            throw new IllegalStateException();
        }
    }

    public WorldState coalesceState(GreedySolver solver) {
        WorldState alr = solver.zobristWorldStateCache.get(worldStateZobristHash);
        if (alr != null) {
            // packedUnrealizedCoordinate can be -1 if the cache did not include our zobrist state on a previous call to coalesceState
            return alr;
        }
        long parent = WorldState.updateZobrist(worldStateZobristHash, packedUnrealizedCoordinate); // updateZobrist is symmetric because XOR, so the same operation can do child->parent as parent->child
        WorldState myState = solver.zobristWorldStateCache.get(parent).withChild(packedUnrealizedCoordinate);
        if (Main.DEBUG && myState.zobristHash != worldStateZobristHash) {
            throw new IllegalStateException();
        }
        solver.zobristWorldStateCache.put(worldStateZobristHash, myState);
        // TODO set packedUnrealizedCoordinate to -1 here perhaps?
        return myState;
    }

    public long pos() {
        return BetterBlockPos.toLong(x, y, z);
    }

    public long nodeMapKey() {
        return pos() ^ worldStateZobristHash;
    }

    public boolean inHeap() {
        return heapPosition != -1;
    }
}
