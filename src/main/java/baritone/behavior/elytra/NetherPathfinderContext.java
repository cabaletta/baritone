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

import dev.babbaj.pathfinder.NetherPathfinder;
import dev.babbaj.pathfinder.PathSegment;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.lang.ref.SoftReference;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Brady
 */
public final class NetherPathfinderContext {

    private final long context;
    private final long seed;
    private final ExecutorService executor;

    public NetherPathfinderContext(long seed) {
        this.context = NetherPathfinder.newContext(seed);
        this.seed = seed;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void queueForPacking(final Chunk chunkIn) {
        final SoftReference<Chunk> ref = new SoftReference<>(chunkIn);
        this.executor.execute(() -> {
            // TODO: Prioritize packing recent chunks and/or ones that the path goes through,
            //       and prune the oldest chunks per chunkPackerQueueMaxSize
            final Chunk chunk = ref.get();
            if (chunk != null) {
                NetherPathfinder.insertChunkData(this.context, chunk.x, chunk.z, pack(chunk));
            }
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

    private static boolean[] pack(Chunk chunk) {
        try {
            boolean[] packed = new boolean[16 * 16 * 128];
            ExtendedBlockStorage[] chunkInternalStorageArray = chunk.getBlockStorageArray();
            for (int y0 = 0; y0 < 8; y0++) {
                ExtendedBlockStorage extendedblockstorage = chunkInternalStorageArray[y0];
                if (extendedblockstorage == null) {
                    continue;
                }
                BlockStateContainer bsc = extendedblockstorage.getData();
                int yReal = y0 << 4;
                for (int y1 = 0; y1 < 16; y1++) {
                    int y = y1 | yReal;
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            IBlockState state = bsc.get(x, y1, z);
                            packed[x | (z << 4) | (y << 8)] = state.getMaterial() != Material.AIR;
                        }
                    }
                }
            }
            return packed;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static final class Visibility {

        public static final int ALL = 0;
        public static final int NONE = 1;
        public static final int ANY = 2;

        private Visibility() {}
    }
}
