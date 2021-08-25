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

public class Node {

    public final long pos;
    public final long worldStateZobristHash;

    public final int heuristic;
    public int cost;
    public int combinedCost;
    public Node previous;
    public int heapPosition;

    // boolean unrealizedZobristBlockChange; // no longer needed since presence in the overall GreedySolver zobristWorldStateCache indicates if this is a yet-unrealized branch of the zobrist space
    private long packedUnrealizedCoordinate;
    // int unrealizedState; // no longer needed now that world state is binarized with scaffolding/build versus air
    // long unrealizedZobristParentHash; // no longer needed since we can compute it backwards with XOR

    public Node(long pos, long zobristState, long unrealizedBlockPlacement, GreedySolver solver, int heuristic) {
        this.pos = pos;
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
            if (Main.DEBUG && alr.zobristHash != worldStateZobristHash) {
                throw new IllegalStateException();
            }
            // don't check packedUnrealizedCoordinate here because it could exist (not -1) if a different route was taken to a zobrist-equivalent node (such as at a different player position) which was then expanded and coalesced
            return alr;
        }
        if (Main.DEBUG && packedUnrealizedCoordinate == -1) {
            throw new IllegalStateException();
        }
        long parent = WorldState.updateZobrist(worldStateZobristHash, packedUnrealizedCoordinate); // updateZobrist is symmetric because XOR, so the same operation can do child->parent as parent->child
        WorldState myState = solver.zobristWorldStateCache.get(parent).withChild(packedUnrealizedCoordinate);
        if (Main.DEBUG && myState.zobristHash != worldStateZobristHash) {
            throw new IllegalStateException();
        }
        solver.zobristWorldStateCache.put(worldStateZobristHash, myState);
        packedUnrealizedCoordinate = -1;
        return myState;
    }

    public long nodeMapKey() {
        return pos ^ worldStateZobristHash;
    }

    public boolean inHeap() {
        return heapPosition != -1;
    }
}
