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

package baritone.behavior.elytra;

import baritone.Baritone;
import baritone.api.event.events.BlockChangeEvent;
import baritone.utils.accessor.IBitArray;
import baritone.utils.accessor.IBlockStateContainer;
import dev.babbaj.pathfinder.NetherPathfinder;
import dev.babbaj.pathfinder.Octree;
import dev.babbaj.pathfinder.PathSegment;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BitArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import javax.annotation.Nonnull;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * @author Brady
 */
public final class NetherPathfinderContext {

    private static final IBlockState AIR_BLOCK_STATE = Blocks.AIR.getDefaultState();
    public final Object cacheLock = new Object();

    // Visible for access in BlockStateOctreeInterface
    final long context;
    private final long seed;
    private final ExecutorService executor;

    public NetherPathfinderContext(long seed) {
        this.context = NetherPathfinder.newContext(seed);
        this.seed = seed;
        this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new WorkQueue());
    }

    public void queueCacheCulling(int chunkX, int chunkZ, int maxDistanceBlocks, BlockStateOctreeInterface boi) {
        this.executor.execute(() -> {
            synchronized (this.cacheLock) {
                boi.chunkPtr = 0L;
                NetherPathfinder.cullFarChunks(this.context, chunkX, chunkZ, maxDistanceBlocks);
            }
        });
    }

    public void queueForPacking(final Chunk chunkIn) {
        final SoftReference<Chunk> ref = new SoftReference<>(chunkIn);
        this.executor.execute(() -> {
            // TODO: Prioritize packing recent chunks and/or ones that the path goes through,
            //       and prune the oldest chunks per chunkPackerQueueMaxSize
            final Chunk chunk = ref.get();
            if (chunk != null) {
                long ptr = NetherPathfinder.getOrCreateChunk(this.context, chunk.x, chunk.z);
                writeChunkData(chunk, ptr);
            }
        });
    }

    public void queueBlockUpdate(BlockChangeEvent event) {
        this.executor.execute(() -> {
            ChunkPos chunkPos = event.getChunkPos();
            long ptr = NetherPathfinder.getChunkPointer(this.context, chunkPos.x, chunkPos.z);
            if (ptr == 0) return; // this shouldn't ever happen
            event.getBlocks().forEach(pair -> {
                BlockPos pos = pair.first();
                if (pos.getY() >= 128) return;
                boolean isSolid = pair.second() != AIR_BLOCK_STATE;
                Octree.setBlock(ptr, pos.getX() & 15, pos.getY(), pos.getZ() & 15, isSolid);
            });
        });
    }

    public CompletableFuture<PathSegment> pathFindAsync(final BlockPos src, final BlockPos dst) {
        return CompletableFuture.supplyAsync(() -> {
            final PathSegment segment = NetherPathfinder.pathFind(
                    this.context,
                    src.getX(), src.getY(), src.getZ(),
                    dst.getX(), dst.getY(), dst.getZ(),
                    true,
                    10000
            );
            if (segment == null) {
                throw new PathCalculationException("Path calculation failed");
            }
            return segment;
        }, this.executor);
    }

    /**
     * Performs a raytrace from the given start position to the given end position, returning {@code true} if there is
     * visibility between the two points.
     *
     * @param startX The start X coordinate
     * @param startY The start Y coordinate
     * @param startZ The start Z coordinate
     * @param endX   The end X coordinate
     * @param endY   The end Y coordinate
     * @param endZ   The end Z coordinate
     * @return {@code true} if there is visibility between the points
     */
    public boolean raytrace(final double startX, final double startY, final double startZ,
                            final double endX, final double endY, final double endZ) {
        return NetherPathfinder.isVisible(this.context, true, startX, startY, startZ, endX, endY, endZ);
    }

    /**
     * Performs a raytrace from the given start position to the given end position, returning {@code true} if there is
     * visibility between the two points.
     *
     * @param start The starting point
     * @param end   The ending point
     * @return {@code true} if there is visibility between the points
     */
    public boolean raytrace(final Vec3d start, final Vec3d end) {
        return NetherPathfinder.isVisible(this.context, true, start.x, start.y, start.z, end.x, end.y, end.z);
    }

    public boolean raytrace(final int count, final double[] src, final double[] dst, final int visibility) {
        switch (visibility) {
            case Visibility.ALL:
                return NetherPathfinder.isVisibleMulti(this.context, true, count, src, dst, false) == -1;
            case Visibility.NONE:
                return NetherPathfinder.isVisibleMulti(this.context, true, count, src, dst, true) == -1;
            case Visibility.ANY:
                return NetherPathfinder.isVisibleMulti(this.context, true, count, src, dst, true) != -1;
            default:
                throw new IllegalArgumentException("lol");
        }
    }

    public void raytrace(final int count, final double[] src, final double[] dst, final boolean[] hitsOut, final double[] hitPosOut) {
        NetherPathfinder.raytrace(this.context, true, count, src, dst, hitsOut, hitPosOut);
    }

    public void cancel() {
        NetherPathfinder.cancel(this.context);
    }

    public void destroy() {
        this.cancel();
        // Ignore anything that was queued up, just shutdown the executor
        this.executor.shutdownNow();

        try {
            while (!this.executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {}
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        NetherPathfinder.freeContext(this.context);
    }

    public long getSeed() {
        return this.seed;
    }

    private static void writeChunkData(Chunk chunk, long ptr) {
        try {
            ExtendedBlockStorage[] chunkInternalStorageArray = chunk.getBlockStorageArray();
            for (int y0 = 0; y0 < 8; y0++) {
                final ExtendedBlockStorage extendedblockstorage = chunkInternalStorageArray[y0];
                if (extendedblockstorage == null) {
                    continue;
                }
                final BlockStateContainer bsc = extendedblockstorage.getData();
                final int airId = ((IBlockStateContainer) bsc).getPalette().idFor(AIR_BLOCK_STATE);
                // pasted from FasterWorldScanner
                final BitArray array = ((IBlockStateContainer) bsc).getStorage();
                if (array == null) continue;
                final long[] longArray = array.getBackingLongArray();
                final int arraySize = array.size();
                final int bitsPerEntry = ((IBitArray) array).getBitsPerEntry();
                final long maxEntryValue = ((IBitArray) array).getMaxEntryValue();

                final int yReal = y0 << 4;
                for (int idx = 0, kl = bitsPerEntry - 1; idx < arraySize; idx++, kl += bitsPerEntry) {
                    final int i = idx * bitsPerEntry;
                    final int j = i >> 6;
                    final int l = i & 63;
                    final int k = kl >> 6;
                    final long jl = longArray[j] >>> l;

                    final int id;
                    if (j == k) {
                        id = (int) (jl & maxEntryValue);
                    } else {
                        id = (int) ((jl | longArray[k] << (64 - l)) & maxEntryValue);
                    }
                    int x = (idx & 15);
                    int y = yReal + (idx >> 8);
                    int z = ((idx >> 4) & 15);
                    Octree.setBlock(ptr, x, y, z, id != airId);
                }
            }
            Octree.setIsFromJava(ptr);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static final class WorkQueue extends TrollBlockingQueue<Runnable> {

        private final LinkedList<Runnable> path;
        private final LinkedList<Runnable> chunk;

        private final ReentrantLock takeLock = new ReentrantLock();
        private final ReentrantLock putLock = new ReentrantLock();
        private final Condition notEmpty = takeLock.newCondition();

        public WorkQueue() {
            this.path = new LinkedList<>();
            this.chunk = new LinkedList<>();
        }

        private void signalNotEmpty() {
            final ReentrantLock takeLock = this.takeLock;
            takeLock.lock();
            try {
                this.notEmpty.signal();
            } finally {
                takeLock.unlock();
            }
        }

        @Override
        public boolean offer(@Nonnull Runnable runnable) {
            final ReentrantLock putLock = this.putLock;
            putLock.lock();
            try {
                if (runnable instanceof ForkJoinTask) {
                    this.path.offer(runnable);
                } else {
                    // Put new chunks at the head of the queue
                    this.chunk.offerFirst(runnable);
                    // Purge oldest chunks
                    while (this.chunk.size() > Baritone.settings().chunkPackerQueueMaxSize.value) {
                        this.chunk.removeLast();
                    }
                }
            } finally {
                putLock.unlock();
            }
            signalNotEmpty();
            return true;
        }

        @Override
        public Runnable take() throws InterruptedException {
            Runnable x;
            final ReentrantLock takeLock = this.takeLock;
            takeLock.lockInterruptibly();
            try {
                while (size() == 0) {
                    notEmpty.await();
                }
                x = dequeue();
                if (!isEmpty()) {
                    notEmpty.signal();
                }
            } finally {
                takeLock.unlock();
            }
            return x;
        }

        @Override
        public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
            Runnable x;
            long nanos = unit.toNanos(timeout);
            final ReentrantLock takeLock = this.takeLock;
            takeLock.lockInterruptibly();
            try {
                while (isEmpty()) {
                    if (nanos <= 0)
                        return null;
                    nanos = notEmpty.awaitNanos(nanos);
                }
                x = dequeue();
                if (!isEmpty())
                    notEmpty.signal();
            } finally {
                takeLock.unlock();
            }
            return x;
        }

        @Override
        public boolean remove(Object o) {
            takeLock.lock();
            putLock.lock();
            try {
                return this.path.remove(o) || this.chunk.remove(o);
            } finally {
                takeLock.unlock();
                putLock.unlock();
            }
        }

        @Override
        public int drainTo(Collection<? super Runnable> c) {
            final ReentrantLock takeLock = this.takeLock;
            takeLock.lock();
            int n = size();
            try {
                if (!this.path.isEmpty()) {
                    c.add(this.path.remove());
                }
                if (!this.chunk.isEmpty()) {
                    c.add(this.chunk.remove());
                }
            } finally {
                takeLock.unlock();
            }
            return n;
        }

        @Override
        public int size() {
            takeLock.lock();
            putLock.lock();
            try {
                return this.path.size() + this.chunk.size();
            } finally {
                takeLock.unlock();
                putLock.unlock();
            }
        }

        @Override
        public boolean isEmpty() {
            return this.size() == 0;
        }

        @SuppressWarnings("unchecked")
        @Override
        public synchronized @Nonnull <T> T[] toArray(@Nonnull T[] a) {
            takeLock.lock();
            putLock.lock();
            try {
                return (T[]) Stream.concat(this.path.stream(), this.chunk.stream()).toArray(Runnable[]::new);
            } finally {
                takeLock.unlock();
                putLock.unlock();
            }
        }

        private Runnable dequeue() {
            return !this.path.isEmpty() ? this.path.remove() : this.chunk.remove();
        }
    }

    @SuppressWarnings("NullableProblems")
    private static class TrollBlockingQueue<T> extends AbstractQueue<T> implements BlockingQueue<T> {

        @Override
        public Iterator<T> iterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void put(T t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean offer(T t, long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public T take() throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public T poll(long timeout, TimeUnit unit) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int remainingCapacity() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int drainTo(Collection<? super T> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int drainTo(Collection<? super T> c, int maxElements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean offer(T t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public T poll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public T peek() {
            throw new UnsupportedOperationException();
        }
    }

    public static final class Visibility {

        public static final int ALL = 0;
        public static final int NONE = 1;
        public static final int ANY = 2;

        private Visibility() {}
    }

    public static boolean isSupported() {
        return NetherPathfinder.isThisSystemSupported();
    }
}
