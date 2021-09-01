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
import it.unimi.dsi.fastutil.longs.LongIterator;

import java.util.BitSet;

public abstract class WorldState {

    /**
     * https://en.wikipedia.org/wiki/Zobrist_hashing
     */
    public final long zobristHash;

    protected WorldState(long zobristHash) {
        this.zobristHash = zobristHash;
    }

    public abstract boolean blockExists(long pos);

    public WorldState withChild(long pos) {
        return new WorldStateLeafDiff(this, pos);
    }

    public static long updateZobrist(long worldStateZobristHash, long changedPosition) {
        return BetterBlockPos.zobrist(changedPosition) ^ worldStateZobristHash;
    }

    public static class WorldStateWrappedSubstrate extends WorldState {

        private final Bounds bounds;
        private final BitSet placed; // won't be copied, since alreadyPlaced is going to be **far** larger than toPlaceNow
        private final int offset;

        public WorldStateWrappedSubstrate(SolverEngineInput inp) {
            super(0L);
            this.bounds = inp.graph.bounds();
            int min;
            int max;
            {
                LongIterator positions = inp.alreadyPlaced.iterator();
                if (!positions.hasNext()) {
                    throw new IllegalStateException();
                }
                min = bounds.toIndex(positions.nextLong());
                max = min;
                while (positions.hasNext()) {
                    int val = bounds.toIndex(positions.nextLong());
                    min = Math.min(min, val);
                    max = Math.max(max, val);
                }
            }
            this.offset = min;
            this.placed = new BitSet(max - min + 1);
            LongIterator it = inp.alreadyPlaced.iterator();
            while (it.hasNext()) {
                placed.set(bounds.toIndex(it.nextLong()) - offset);
            }
        }

        @Override
        public boolean blockExists(long pos) {
            int storedAt = bounds.toIndex(pos) - offset;
            if (storedAt < 0) {
                return false;
            }
            return placed.get(storedAt);
        }
    }

    public static class WorldStateLeafDiff extends WorldState {

        private WorldState delegate;
        private final long pos;

        private WorldStateLeafDiff(WorldState delegate, long pos) {
            super(updateZobrist(delegate.zobristHash, pos));
            this.delegate = delegate;
            this.pos = pos;
        }

        @Override
        public boolean blockExists(long pos) {
            return this.pos == pos || delegate.blockExists(pos);
        }
    }
}
