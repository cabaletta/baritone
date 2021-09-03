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
    int heapPosition;

    // boolean unrealizedZobristBlockChange; // no longer needed since presence in the overall GreedySolver zobristWorldStateCache indicates if this is a yet-unrealized branch of the zobrist space
    long packedUnrealizedCoordinate;
    // int unrealizedState; // no longer needed now that world state is binarized with scaffolding/build versus air
    // long unrealizedZobristParentHash; // no longer needed since we can compute it backwards with XOR

    public Node(long pos, Face sneakingTowards, long zobristState, long unrealizedBlockPlacement, int heuristic) {
        this.posAndSneak = SneakPosition.encode(pos, sneakingTowards);
        this.heapPosition = -1;
        this.cost = Integer.MAX_VALUE;
        this.heuristic = heuristic;
        this.worldStateZobristHash = zobristState;
        this.packedUnrealizedCoordinate = unrealizedBlockPlacement;
    }

    public boolean sneaking() {
        return SneakPosition.hasSneak(posAndSneak);
    }

    public long pos() {
        return posAndSneak & BetterBlockPos.POST_ADDITION_MASK;
    }

    public long sneakingPosition() {
        return posAndSneak;
    }

    public long nodeMapKey() {
        return posAndSneak ^ worldStateZobristHash;
    }

    public boolean inHeap() {
        return heapPosition >= 0;
    }
}
