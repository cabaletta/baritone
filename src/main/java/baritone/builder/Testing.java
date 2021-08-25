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
import baritone.utils.BlockStateInterface;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

import java.util.Iterator;

/**
 * Just some testing stuff
 */
public class Testing {

    private static final int MAX_LEAF_LEVEL = 16;

    public static class AbstractNodeCostSearch {

        long[] KEYS_CACHE = new long[MAX_LEAF_LEVEL];
        int[] VALS_CACHE = new int[MAX_LEAF_LEVEL];
    }


    public static abstract class BlockStateInterfaceAbstractWrapper {

        protected long zobrist;

        public abstract IBlockState get(long coord);

        public IBlockState get1(int x, int y, int z) {
            return get(BetterBlockPos.toLong(x, y, z));
        }

        public BlockStateInterfaceAbstractWrapper with(long packedCoordinate, int state, AbstractNodeCostSearch ref) {
            return new BlockStateInterfaceLeafDiff(this, packedCoordinate, Block.BLOCK_STATE_IDS.getByValue(state));
        }

    }

    public static class BlockStateInterfaceLeafDiff extends BlockStateInterfaceAbstractWrapper {

        private BlockStateInterfaceAbstractWrapper delegate;
        private final long coord;
        private final IBlockState state;

        public BlockStateInterfaceLeafDiff(BlockStateInterfaceAbstractWrapper delegate, long coord, IBlockState state) {
            this.delegate = delegate;
            this.state = state;
            this.coord = coord;
            zobrist = updateZobristHash(delegate.zobrist, coord, state, delegate.get(coord));
        }

        public static long updateZobristHash(long oldZobristHash, long coord, IBlockState emplaced, IBlockState old) {
            if (old != null) {
                return updateZobristHash(updateZobristHash(oldZobristHash, coord, emplaced, null), coord, old, null);
            }
            return oldZobristHash ^ BetterBlockPos.murmur64(BetterBlockPos.longHash(coord) ^ Block.BLOCK_STATE_IDS.get(emplaced));
        }

        @Override
        public IBlockState get(long coord) {
            if (this.coord == coord) {
                return state;
            }
            return delegate.get(coord);
        }

        public int leafLevels() { // cant cache leaflevel because of how materialize changes it
            return 1 + (delegate instanceof BlockStateInterfaceLeafDiff ? ((BlockStateInterfaceLeafDiff) delegate).leafLevels() : 0);
        }

        @Override
        public BlockStateInterfaceAbstractWrapper with(long packedCoordinate, int state, AbstractNodeCostSearch ref) {
            int level = leafLevels();
            if (level < MAX_LEAF_LEVEL) {
                return super.with(packedCoordinate, state, ref);
            }
            if (level > MAX_LEAF_LEVEL) {
                throw new IllegalStateException();
            }
            BlockStateInterfaceLeafDiff parent = this;
            for (int i = 0; i < MAX_LEAF_LEVEL / 2; i++) {
                parent = (BlockStateInterfaceLeafDiff) parent.delegate;
            }
            parent.delegate = ((BlockStateInterfaceLeafDiff) parent.delegate).materialize(ref);
            return super.with(packedCoordinate, state, ref);
        }

        public BlockStateInterfaceMappedDiff materialize(AbstractNodeCostSearch ref) {
            BlockStateInterfaceLeafDiff parent = this;
            int startIdx = MAX_LEAF_LEVEL;
            BlockStateInterfaceMappedDiff ancestor;
            long[] keys = ref.KEYS_CACHE;
            int[] vals = ref.VALS_CACHE;
            while (true) {
                startIdx--;
                keys[startIdx] = parent.coord;
                vals[startIdx] = Block.BLOCK_STATE_IDS.get(parent.state);
                if (parent.delegate instanceof BlockStateInterfaceLeafDiff) {
                    parent = (BlockStateInterfaceLeafDiff) parent.delegate;
                } else {
                    if (parent.delegate instanceof BlockStateInterfaceMappedDiff) {
                        ancestor = (BlockStateInterfaceMappedDiff) parent.delegate;
                    } else {
                        ancestor = new BlockStateInterfaceMappedDiff((BlockStateInterfaceWrappedSubstrate) parent.delegate);
                    }
                    break;
                }
            }
            BlockStateInterfaceMappedDiff coalesced = new BlockStateInterfaceMappedDiff(ancestor, keys, vals, startIdx, zobrist);
            //ref.zobristMap.put(zobrist, coalesced);
            return coalesced;
        }
    }

    public static class BlockStateInterfaceMappedDiff extends BlockStateInterfaceAbstractWrapper {

        private final BlockStateInterfaceWrappedSubstrate delegate;
        private final Long2ObjectOpenHashMap<Long2IntOpenHashMap> sections;
        private static final long MASK = BetterBlockPos.toLong(15, 15, 15);

        public BlockStateInterfaceMappedDiff(BlockStateInterfaceWrappedSubstrate substrate) {
            this.delegate = substrate;
            this.sections = new Long2ObjectOpenHashMap<>();
        }

        public BlockStateInterfaceMappedDiff(BlockStateInterfaceMappedDiff delegate, long[] diffKeys, int[] diffVals, int startIdx, long zobrist) {
            this.sections = new Long2ObjectOpenHashMap<>();
            this.zobrist = zobrist;
            this.delegate = delegate.delegate;
            for (int i = startIdx; i < diffKeys.length; i++) {
                long bucket = diffKeys[i] & ~MASK;
                Long2IntOpenHashMap val = sections.get(bucket);
                if (val == null) {
                    Long2IntOpenHashMap parent = delegate.sections.get(bucket);
                    if (parent == null) {
                        val = new Long2IntOpenHashMap();
                        val.defaultReturnValue(-1);
                    } else {
                        val = parent.clone();
                    }
                    sections.put(bucket, val);
                }
                val.put(diffKeys[i] & MASK, diffVals[i]);
            }
            Iterator<Long2ObjectMap.Entry<Long2IntOpenHashMap>> it = delegate.sections.long2ObjectEntrySet().fastIterator();
            while (it.hasNext()) {
                Long2ObjectMap.Entry<Long2IntOpenHashMap> entry = it.next();
                if (sections.containsKey(entry.getLongKey())) {
                    continue;
                }
                sections.put(entry.getLongKey(), entry.getValue());
            }
        }

        @Override
        public IBlockState get(long coord) {
            Long2IntOpenHashMap candidateSection = sections.get(coord & ~MASK);
            if (candidateSection != null) {
                int val = candidateSection.get(coord & MASK);
                if (val != -1) {
                    return Block.BLOCK_STATE_IDS.getByValue(val);
                }
            }
            return delegate.get(coord);
        }
    }

    public static class BlockStateInterfaceWrappedSubstrate extends BlockStateInterfaceAbstractWrapper {

        private BlockStateInterface delegate;

        @Override
        public IBlockState get(long coord) {
            return delegate.get0(BetterBlockPos.XfromLong(coord), BetterBlockPos.YfromLong(coord), BetterBlockPos.ZfromLong(coord));
        }

        /*@Override
        public IBlockState getOverridden(long coord) {
            return null;
        }*/
    }

    public static class BlockStateLookupHelper {

        // this falls back from System.identityHashCode to == so it's safe even in the case of a 32-bit collision
        // but that would never have happened anyway: https://www.wolframalpha.com/input/?i=%28%282%5E32-1%29%2F%282%5E32%29%29%5E2000
        private static Reference2IntOpenHashMap<IBlockState> states = new Reference2IntOpenHashMap<>();

        static {
            states.defaultReturnValue(-1); // normal default is 0
        }

        public static int lookupBlockState(IBlockState state) {
            int stateMaybe = states.getInt(state);
            if (stateMaybe >= 0) {
                return stateMaybe;
            }
            int realState = Block.BLOCK_STATE_IDS.get(state); // uses slow REAL hashcode that walks through the Map of properties, gross
            states.put(state, realState);
            return realState;
        }
    }
}
