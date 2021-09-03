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

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class ZobristWorldStateCache {

    private final Long2ObjectOpenHashMap<WorldState> zobristWorldStateCache;

    public ZobristWorldStateCache(WorldState zeroEntry) {
        this.zobristWorldStateCache = new Long2ObjectOpenHashMap<>();
        this.zobristWorldStateCache.put(0L, zeroEntry);
    }

    public WorldState coalesceState(Node node) {
        WorldState alr = zobristWorldStateCache.get(node.worldStateZobristHash);
        if (alr != null) {
            if (Main.DEBUG && alr.zobristHash != node.worldStateZobristHash) {
                throw new IllegalStateException();
            }
            // don't check packedUnrealizedCoordinate here because it could exist (not -1) if a different route was taken to a zobrist-equivalent node (such as at a different player position) which was then expanded and coalesced
            return alr;
        }
        if (Main.DEBUG && node.packedUnrealizedCoordinate == -1) {
            throw new IllegalStateException();
        }
        long parent = WorldState.updateZobrist(node.worldStateZobristHash, node.packedUnrealizedCoordinate); // updateZobrist is symmetric because XOR, so the same operation can do child->parent as parent->child
        WorldState myState = zobristWorldStateCache.get(parent).withChild(node.packedUnrealizedCoordinate);
        if (Main.DEBUG && myState.zobristHash != node.worldStateZobristHash) {
            throw new IllegalStateException();
        }
        zobristWorldStateCache.put(node.worldStateZobristHash, myState);
        node.packedUnrealizedCoordinate = -1;
        return myState;
    }
}
