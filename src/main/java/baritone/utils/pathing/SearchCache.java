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

package baritone.utils.pathing;

import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.function.IntFunction;

public final class SearchCache {

    private static final Pool<MiningBuffers> MINING = new Pool<>(MiningBuffers::new);
    private static final Pool<BlockBuffers> BLOCKS = new Pool<>(BlockBuffers::new);

    private SearchCache() {}

    public static int bitsForSize(int requestedSize) {
        int size = Math.max(1024, Math.min(65536, requestedSize));
        return Integer.SIZE - Integer.numberOfLeadingZeros(size - 1);
    }

    public static MiningBuffers acquireMining(int size) {
        return MINING.acquire(size);
    }

    public static BlockBuffers acquireBlocks(int size) {
        return BLOCKS.acquire(size);
    }

    public static void release(MiningBuffers buffers) {
        MINING.release(buffers);
    }

    public static void release(BlockBuffers buffers) {
        BLOCKS.release(buffers);
    }

    public abstract static class Buffers {
        public final int bits;
        volatile Thread owner;

        private Buffers(int bits) {
            this.bits = bits;
        }

        abstract void clearKeys();
    }

    public static final class MiningBuffers extends Buffers {
        public final long[] keys;
        public final long[] fallingKeys;
        public final double[] values;
        public final double[] fallingValues;

        private MiningBuffers(int bits) {
            super(bits);
            keys = new long[1 << bits];
            fallingKeys = new long[1 << bits];
            values = new double[1 << bits];
            fallingValues = new double[1 << bits];
        }

        @Override
        void clearKeys() {
            Arrays.fill(keys, -1L);
            Arrays.fill(fallingKeys, -1L);
        }
    }

    public static final class BlockBuffers extends Buffers {
        public final long[] keys;
        public final BlockState[] values;

        private BlockBuffers(int bits) {
            super(bits);
            keys = new long[1 << bits];
            values = new BlockState[1 << bits];
        }

        @Override
        void clearKeys() {
            Arrays.fill(keys, -1L);
        }
    }

    private static final class Pool<T extends Buffers> {
        private final ThreadLocal<T> current = new ThreadLocal<>();
        private final IntFunction<T> factory;

        private Pool(IntFunction<T> factory) {
            this.factory = factory;
        }

        private T acquire(int size) {
            int bits = bitsForSize(size);
            T cached = current.get();
            T buffers = cached;
            if (buffers == null || buffers.owner != null || buffers.bits != bits) {
                buffers = factory.apply(bits);
                // a nested search gets its own arrays. don't evict the outer search's reusable ones
                if (cached == null || cached.owner == null) {
                    current.set(buffers);
                }
            }
            // values can stay, but keys can't. stone might be air in the next search
            buffers.clearKeys();
            buffers.owner = Thread.currentThread();
            return buffers;
        }

        private void release(T buffers) {
            if (buffers.owner == Thread.currentThread()) {
                buffers.owner = null;
            }
        }
    }
}
