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
import baritone.pathing.calc.openset.BinaryHeapOpenSet;
import baritone.utils.BlockStateInterface;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

import java.util.Iterator;

/**
 * Just some testing stuff
 */
public class Testing {

    private static final int MAX_LEAF_LEVEL = 16;

    public static class AbstractNodeCostSearch {

        Long2ObjectOpenHashMap<PathNode> nodeMap;
        BinaryHeapOpenSet openSet;
        Long2ObjectOpenHashMap<BlockStateInterfaceAbstractWrapper> zobristMap;

        long[] KEYS_CACHE = new long[MAX_LEAF_LEVEL];
        int[] VALS_CACHE = new int[MAX_LEAF_LEVEL];

        protected void search() {
            while (!openSet.isEmpty()) {
                PathNode node = null;
                BlockStateInterfaceAbstractWrapper bsi = node.get(this);

                // consider actions:
                // traverse
                // pillar
                // place blocks beneath feet
            }
        }

        /*protected PathNode getNodeAtPosition(int x, int y, int z, long hashCode) {
            baritone.pathing.calc.PathNode node = map.get(hashCode);
            if (node == null) {
                node = new baritone.pathing.calc.PathNode(x, y, z, goal);
                map.put(hashCode, node);
            }
            return node;
        }*/
    }


    public static final class PathNode {

        public int x;
        public int y;
        public int z;

        public long zobristBlocks;

        public int heuristic;
        public int cost;
        public int combinedCost;
        public PathNode previous;
        public int heapPosition;

        boolean unrealizedZobristBlockChange;
        long packedUnrealizedCoordinate;
        int unrealizedState;
        long unrealizedZobristParentHash;

        public long nodeMapKey() {
            return BetterBlockPos.longHash(x, y, z) ^ zobristBlocks;
        }

        public BlockStateInterfaceAbstractWrapper get(AbstractNodeCostSearch ref) {
            BlockStateInterfaceAbstractWrapper alr = ref.zobristMap.get(zobristBlocks);
            if (alr != null) {
                return alr;
            }
            if (!unrealizedZobristBlockChange || (BetterBlockPos.murmur64(BetterBlockPos.longHash(packedUnrealizedCoordinate) ^ unrealizedState) ^ zobristBlocks) != unrealizedZobristParentHash) {
                throw new IllegalStateException();
            }
            alr = ref.zobristMap.get(unrealizedZobristParentHash).with(packedUnrealizedCoordinate, unrealizedState, ref);
            if (alr.zobrist != zobristBlocks) {
                throw new IllegalStateException();
            }
            ref.zobristMap.put(zobristBlocks, alr);
            return alr;
        }
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
            ref.zobristMap.put(zobrist, coalesced);
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


}
