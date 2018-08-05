package baritone.bot.chunk;

import baritone.bot.pathing.util.PathingBlockType;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.BitSet;

/**
 * @author Brady
 * @since 8/4/2018 12:02 AM
 */
public final class CachedWorld implements ICachedChunkAccess {

    /**
     * The maximum number of regions in any direction from (0,0)
     */
    private static final int REGION_MAX = 117188;

    /**
     * A map of all of the cached regions.
     */
    private Long2ObjectMap<CachedRegion> cachedRegions = new Long2ObjectOpenHashMap<>();

    /**
     * The directory that the cached region files are saved to
     */
    private final String directory;

    public CachedWorld(String directory) {
        this.directory = directory;
        // Insert an invalid region element
        cachedRegions.put(0, null);
    }

    @Override
    public final PathingBlockType getBlockType(int x, int y, int z) {
        CachedRegion region = getRegion(x >> 9, z >> 9);
        if (region != null) {
            return region.getBlockType(x, y, z);
        }
        return null;
    }

    @Override
    public final void updateCachedChunk(int chunkX, int chunkZ, BitSet data) {
        CachedRegion region = getOrCreateRegion(chunkX >> 5, chunkZ >> 5);
        if (region != null) {
            region.updateCachedChunk(chunkX & 31, chunkZ & 31, data);
        }
    }

    public final void save() {
        this.cachedRegions.values().forEach(region -> {
            if (region != null)
                region.save(this.directory);
        });
    }

    public final void load() {
        this.cachedRegions.values().forEach(region -> {
            if (region != null)
                region.load(this.directory);
        });
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
    private CachedRegion getOrCreateRegion(int regionX, int regionZ) {
        return cachedRegions.computeIfAbsent(getRegionID(regionX, regionZ), id -> {
            CachedRegion newRegion = new CachedRegion(regionX, regionZ);
            newRegion.load(this.directory);
            return newRegion;
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
}
