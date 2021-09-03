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
        this.posAndSneak = encode(pos, sneakingTowards);
        this.heapPosition = -1;
        this.cost = Integer.MAX_VALUE;
        this.heuristic = heuristic;
        this.worldStateZobristHash = zobristState;
        this.packedUnrealizedCoordinate = unrealizedBlockPlacement;
    }

    public static long encode(long pos, Face sneakingTowards) {
        if (Main.DEBUG && sneakingTowards != null && sneakingTowards.vertical) {
            throw new IllegalStateException();
        }
        return sneakingTowards == null ? pos : encode(pos, sneakingTowards.horizontalIndex);
    }

    public static long encode(long pos, int sneak) {
        if (Main.DEBUG && (sneak < 0 || sneak > 3)) {
            throw new IllegalStateException();
        }
        if (Main.DEBUG && ((pos & BetterBlockPos.POST_ADDITION_MASK) != pos)) {
            throw new IllegalStateException();
        }
        long ret = pos
                | (sneak & 0x1L) << 26 // snugly and cozily fit into the two bits left between Y and Z
                | (sneak & 0x2L) << 35 // and between X and Y
                | 1L << 63; // and turn on the top bit as a signal
        if (Main.DEBUG && ((ret & BetterBlockPos.POST_ADDITION_MASK) != pos)) { // ensure that POST_ADDITION_MASK undoes this trickery (see the comments around POST_ADDITION_MASK definition for why/how)
            throw new IllegalStateException();
        }
        return ret;
    }

    public static int decode(long posAndSneak) {
        if (Main.DEBUG && !hasSneak(posAndSneak)) {
            throw new IllegalStateException();
        }
        return (int) (posAndSneak >> 26 & 0x1L | posAndSneak >> 35 & 0x2L);
    }

    public boolean sneaking() {
        return hasSneak(posAndSneak);
    }

    public static boolean hasSneak(long posAndSneak) {
        return posAndSneak < 0; // checks the MSB like a boss (epically)
    }

    public long pos() {
        return posAndSneak & BetterBlockPos.POST_ADDITION_MASK;
    }

    public Face sneakDirectionFromPlayerToSupportingBlock() {
        if (hasSneak(posAndSneak)) {
            return Face.HORIZONTALS[Face.oppositeHorizontal(decode(posAndSneak))];
        } else {
            return null;
        }
    }

    public Face sneakDirectionFromSupportingBlockToPlayer() {
        if (hasSneak(posAndSneak)) {
            return Face.HORIZONTALS[decode(posAndSneak)];
        } else {
            return null;
        }
    }

    public long nodeMapKey() {
        return posAndSneak ^ worldStateZobristHash;
    }

    public boolean inHeap() {
        return heapPosition != -1;
    }
}
