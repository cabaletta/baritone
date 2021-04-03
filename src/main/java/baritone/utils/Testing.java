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

package baritone.utils;

import baritone.api.utils.BetterBlockPos;
import baritone.pathing.calc.openset.BinaryHeapOpenSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

public class Testing {
    public static class AbstractNodeCostSearch {
        Long2ObjectOpenHashMap<PathNode> nodeMap;
        BinaryHeapOpenSet openSet;
        Long2ObjectOpenHashMap<BlockStateInterface> zobristMap;

        protected PathNode getNodeAtPosition(int x, int y, int z, long hashCode) {
            baritone.pathing.calc.PathNode node = map.get(hashCode);
            if (node == null) {
                node = new baritone.pathing.calc.PathNode(x, y, z, goal);
                map.put(hashCode, node);
            }
            return node;
        }
    }


    public static final class PathNode {
        public int x;
        public int y;
        public int z;

        public long zobristBlocks;

        public int estimatedCostToGoal;
        public int cost;
        public int combinedCost;
        public PathNode previous;
        public int heapPosition;

        public long nodeMapKey() {
            return BetterBlockPos.longHash(x, y, z) ^ zobristBlocks;
        }
    }

    public static abstract class BlockStateInterfaceAbstractWrapper {
        protected long zobrist;

        public abstract IBlockState get(int x, int y, int z);

    }

    public static class BlockStateInterfaceLeafDiff extends BlockStateInterfaceAbstractWrapper {
        private BlockStateInterfaceAbstractWrapper delegate;
        int x;
        int y;
        int z;
        IBlockState state;

        @Override
        public IBlockState get(int x, int y, int z) {
            if (x == this.x && y == this.y && z == this.z) {
                return state;
            }
            return delegate.get(x, y, z);
        }

        public int leafLevels() {
            return 1 + (delegate instanceof BlockStateInterfaceLeafDiff ? ((BlockStateInterfaceLeafDiff) delegate).leafLevels() : 0);
        }

        public static long updateZobrist(long oldZobrist, int x, int y, int z, IBlockState emplaced, IBlockState old) {
            if (old != null) {
                return updateZobrist(updateZobrist(oldZobrist, x, y, z, emplaced, null), x, y, z, old, null);
            }
            return oldZobrist ^ BetterBlockPos.murmur64(BetterBlockPos.longHash(x, y, z) ^ Block.BLOCK_STATE_IDS.get(emplaced));
        }
    }

    public static class BlockStateInterfaceWrappedSubstrate extends BlockStateInterfaceAbstractWrapper {
        private BlockStateInterface delegate;

        @Override
        public IBlockState get(int x, int y, int z) {
            return delegate.get0(x, y, z);
        }
    }


}
