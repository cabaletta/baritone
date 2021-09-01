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

    private final long posAndSneak;
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

    public static long encode(long pos, int sneak) {
        if (Main.DEBUG && (sneak < 0 || sneak > 3)) {
            throw new IllegalStateException();
        }
        if (Main.DEBUG && ((pos & BetterBlockPos.POST_ADDITION_MASK) != pos)) {
            throw new IllegalStateException();
        }
        long ret = pos
                | (sneak & 0x1L) << 26 // snugly and cozily fit into the two bits left between X and Y and between Y and Z
                | (sneak & 0x2L) << 35
                | 1L << 63; // and turn on the top bit as a signal
        if (Main.DEBUG && ((ret & BetterBlockPos.POST_ADDITION_MASK) != pos)) {
            throw new IllegalStateException();
        }
        return ret;
    }

    public long pos() {
        return posAndSneak & BetterBlockPos.POST_ADDITION_MASK;
    }

    public long nodeMapKey() {
        return posAndSneak ^ worldStateZobristHash;
    }

    public boolean inHeap() {
        return heapPosition != -1;
    }
}
