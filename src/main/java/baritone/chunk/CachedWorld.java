/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.chunk;

import baritone.Baritone;
import baritone.utils.pathing.IBlockTypeAccess;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * @author Brady
 * @since 8/4/2018 12:02 AM
 */
public final class CachedWorld implements IBlockTypeAccess {

    /**
     * The maximum number of regions in any direction from (0,0)
     */
    private static final int REGION_MAX = 58594;

    /**
     * A map of all of the cached regions.
     */
    private Long2ObjectMap<CachedRegion> cachedRegions = new Long2ObjectOpenHashMap<>();

    /**
     * The directory that the cached region files are saved to
     */
    private final String directory;

    private final LinkedBlockingQueue<Chunk> toPack = new LinkedBlockingQueue<>();

    CachedWorld(Path directory) {
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException ignored) {
            }
        }
        this.directory = directory.toString();
        System.out.println("Cached world directory: " + directory);
        // Insert an invalid region element
        cachedRegions.put(0, null);
        new PackerThread().start();
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(30000);
                    while (true) {
                        // since a region only saves if it's been modified since its last save
                        // saving every 10 minutes means that once it's time to exit
                        // we'll only have a couple regions to save
                        save();
                        Thread.sleep(600000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public final void queueForPacking(Chunk chunk) {
        try {
            toPack.put(chunk);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public final IBlockState getBlock(int x, int y, int z) {
        // no point in doing getOrCreate region, if we don't have it we don't have it
        CachedRegion region = getRegion(x >> 9, z >> 9);
        if (region == null) {
            return null;
        }
        return region.getBlock(x & 511, y, z & 511);
    }

    public final boolean isCached(BlockPos pos) {
        int x = pos.getX();
        int z = pos.getZ();
        CachedRegion region = getRegion(x >> 9, z >> 9);
        if (region == null) {
            return false;
        }
        return region.isCached(x & 511, z & 511);
    }


    public final LinkedList<BlockPos> getLocationsOf(String block, int minimum, int maxRegionDistanceSq) {
        LinkedList<BlockPos> res = new LinkedList<>();
        int playerRegionX = playerFeet().getX() >> 9;
        int playerRegionZ = playerFeet().getZ() >> 9;

        int searchRadius = 0;
        while (searchRadius <= maxRegionDistanceSq) {
            for (int xoff = -searchRadius; xoff <= searchRadius; xoff++) {
                for (int zoff = -searchRadius; zoff <= searchRadius; zoff++) {
                    int distance = xoff * xoff + zoff * zoff;
                    if (distance != searchRadius) {
                        continue;
                    }
                    int regionX = xoff + playerRegionX;
                    int regionZ = zoff + playerRegionZ;
                    CachedRegion region = getOrCreateRegion(regionX, regionZ);
                    if (region != null)
                        for (BlockPos pos : region.getLocationsOf(block))
                            res.add(pos);
                }
            }
            if (res.size() >= minimum) {
                return res;
            }
            searchRadius++;
        }
        return res;
    }

    private void updateCachedChunk(CachedChunk chunk) {
        CachedRegion region = getOrCreateRegion(chunk.x >> 5, chunk.z >> 5);
        region.updateCachedChunk(chunk.x & 31, chunk.z & 31, chunk);
    }

    public final void save() {
        if (!Baritone.settings().chunkCaching.get()) {
            System.out.println("Not saving to disk; chunk caching is disabled.");
            return;
        }
        long start = System.nanoTime() / 1000000L;
        this.cachedRegions.values().parallelStream().forEach(region -> {
            if (region != null)
                region.save(this.directory);
        });
        long now = System.nanoTime() / 1000000L;
        System.out.println("World save took " + (now - start) + "ms");
    }

    public final void reloadAllFromDisk() {
        long start = System.nanoTime() / 1000000L;
        this.cachedRegions.values().forEach(region -> {
            if (region != null)
                region.load(this.directory);
        });
        long now = System.nanoTime() / 1000000L;
        System.out.println("World load took " + (now - start) + "ms");
    }

    /**
     * Returns the region at the specified region coordinates
     *
     * @param regionX The region X coordinate
     * @param regionZ The region Z coordinate
     * @return The region located at the specified coordinates
     */
    public final CachedRegion getRegion(int regionX, int regionZ) {
        return cachedRegions.get(getRegionID(regionX, regionZ));
    }

    /**
     * Returns the region at the specified region coordinates. If a
     * region is not found, then a new one is created.
     *
     * @param regionX The region X coordinate
     * @param regionZ The region Z coordinate
     * @return The region located at the specified coordinates
     */
    private synchronized CachedRegion getOrCreateRegion(int regionX, int regionZ) {
        return cachedRegions.computeIfAbsent(getRegionID(regionX, regionZ), id -> {
            CachedRegion newRegion = new CachedRegion(regionX, regionZ);
            newRegion.load(this.directory);
            return newRegion;
        });
    }

    public void forEachRegion(Consumer<CachedRegion> consumer) {
        this.cachedRegions.forEach((id, r) -> {
            if (r != null)
                consumer.accept(r);
        });
    }

    /**
     * Returns the region ID based on the region coordinates. 0 will be
     * returned if the specified region coordinates are out of bounds.
     *
     * @param regionX The region X coordinate
     * @param regionZ The region Z coordinate
     * @return The region ID
     */
    private long getRegionID(int regionX, int regionZ) {
        if (!isRegionInWorld(regionX, regionZ))
            return 0;

        return (long) regionX & 0xFFFFFFFFL | ((long) regionZ & 0xFFFFFFFFL) << 32;
    }

    /**
     * Returns whether or not the specified region coordinates is within the world bounds.
     *
     * @param regionX The region X coordinate
     * @param regionZ The region Z coordinate
     * @return Whether or not the region is in world bounds
     */
    private boolean isRegionInWorld(int regionX, int regionZ) {
        return regionX <= REGION_MAX && regionX >= -REGION_MAX && regionZ <= REGION_MAX && regionZ >= -REGION_MAX;
    }

    private class PackerThread extends Thread {
        public void run() {
            while (true) {
                LinkedBlockingQueue<Chunk> queue = toPack;
                if (queue == null) {
                    break;
                }
                try {
                    Chunk chunk = queue.take();
                    CachedChunk cached = ChunkPacker.pack(chunk);
                    CachedWorld.this.updateCachedChunk(cached);
                    //System.out.println("Processed chunk at " + chunk.x + "," + chunk.z);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
}
