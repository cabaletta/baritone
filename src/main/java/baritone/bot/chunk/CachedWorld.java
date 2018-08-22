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

package baritone.bot.chunk;

import baritone.bot.utils.pathing.IBlockTypeAccess;
import baritone.bot.utils.pathing.PathingBlockType;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.chunk.Chunk;

import java.util.BitSet;
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

    public CachedWorld(String directory) {
        this.directory = directory;
        // Insert an invalid region element
        cachedRegions.put(0, null);
        new PackerThread().start();
    }

    public final void queueForPacking(Chunk chunk) {
        try {
            toPack.put(chunk);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public final PathingBlockType getBlockType(int x, int y, int z) {
        CachedRegion region = getOrCreateRegion(x >> 9, z >> 9);
        return region.getBlockType(x & 511, y, z & 511);
    }

    private void updateCachedChunk(int chunkX, int chunkZ, BitSet data) {
        CachedRegion region = getOrCreateRegion(chunkX >> 5, chunkZ >> 5);
        region.updateCachedChunk(chunkX & 31, chunkZ & 31, data);
    }

    public final void save() {
        long start = System.currentTimeMillis();
        this.cachedRegions.values().forEach(region -> {
            if (region != null)
                region.save(this.directory);
        });
        long now = System.currentTimeMillis();
        System.out.println("World save took " + (now - start) + "ms");
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
    CachedRegion getOrCreateRegion(int regionX, int regionZ) {
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
                    BitSet packed = ChunkPacker.createPackedChunk(chunk);
                    CachedWorld.this.updateCachedChunk(chunk.x, chunk.z, packed);
                    //System.out.println("Processed chunk at " + chunk.x + "," + chunk.z);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
}
