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

package baritone.process.elytra.pathfinder;

import net.minecraft.core.BlockPos;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A pathfinder and raytracer over a cache of one-bit-per-block chunks, for flying the nether by
 * elytra. One instance is what the native library called a context: a world's seed and dimension,
 * the chunks it has been given or has generated, and a search that can be cancelled.
 * <p>
 * The method names are the ones the native API had, with objects where it had pointers: a chunk
 * is a {@link Chunk} that stays valid for as long as it is referenced, and there is nothing to free.
 * Lookups, inserts, the search and rays may all run at the same time from different threads.
 */
public final class NetherPathfinder implements AutoCloseable {

    /** What a ray or a search makes of a chunk the table does not hold. */
    public enum CacheMiss {
        /** Generate it from the seed, and keep it. */
        GENERATE,
        /** Treat it as all air. */
        AIR,
        /** Treat it as all solid. */
        SOLID
    }

    public enum Dimension {
        OVERWORLD, NETHER, END
    }

    static final int STATE_FROM_CALLER = 0;
    static final int STATE_FAKE = 1; // could be generated or just air

    /** A chunk in the table, where it is, and whether it came from the game or was made up. */
    static final class Entry {
        final Chunk chunk;
        final int x;
        final int z;
        volatile int state;

        Entry(int state, Chunk chunk, int x, int z) {
            this.state = state;
            this.chunk = chunk;
            this.x = x;
            this.z = z;
        }
    }

    private static final Entry AIR_ENTRY = new Entry(STATE_FAKE, Chunk.AIR, 0, 0);

    private final ConcurrentHashMap<Long, Entry> chunks = new ConcurrentHashMap<>();
    private final Set<Long> checkedRegions = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean cancelFlag = new AtomicBoolean();
    private final ChunkGeneratorHell generator;
    private final String baritoneCache;
    private final long seed;
    private final Dimension dimension;
    final int maxHeight;

    /**
     * @param seed                      the world seed, for generating terrain where no chunk is known
     * @param baritoneCacheDirCanBeNull Baritone's region directory for the dimension, whose chunks a search loads as it reaches them; or null
     * @param dimension                 the dimension the chunks are from, which decides their height
     * @param maxHeight                 the height the search and rays stay under, 1 to 384
     */
    public NetherPathfinder(long seed, String baritoneCacheDirCanBeNull, Dimension dimension, int maxHeight) {
        Objects.requireNonNull(dimension, "dimension");
        if (maxHeight <= 0 || maxHeight > Chunk.HEIGHT) {
            throw new IllegalArgumentException("Invalid max height (must be between 0 and 384)");
        }
        this.seed = seed;
        this.baritoneCache = baritoneCacheDirCanBeNull;
        this.dimension = dimension;
        this.maxHeight = maxHeight;
        this.generator = ChunkGeneratorHell.fromSeed(seed);
    }

    public long getSeed() {
        return this.seed;
    }

    public Dimension getDimension() {
        return this.dimension;
    }

    public int getMaxHeight() {
        return this.maxHeight;
    }

    @Override
    public void close() {
        cancel();
        this.chunks.clear();
        this.checkedRegions.clear();
    }

    /**
     * The table's key for chunk (x, z). The pair packed into a long would do, but the table hashes a
     * Long to its upper half xor its lower half, x ^ z, which over the chunks around a player takes
     * a few dozen values and turns the table's bins into trees that every lookup then walks; so the
     * packed pair is multiplied by an odd constant, a bijection that spreads every bit of both
     * halves over the upper one. The key is not decoded anywhere: an entry knows its own position.
     */
    static long key(int x, int z) {
        return (((long) x << 32) | (z & 0xFFFFFFFFL)) * 0x9E3779B97F4A7C15L;
    }

    static int dimensionHeight(Dimension dimension) {
        return dimension == Dimension.OVERWORLD ? 384 : 256;
    }

    private static boolean inBounds(int y) {
        return y >= 0 && y < Chunk.HEIGHT;
    }


    // ---- the chunk table ----

    /**
     * Inserts a chunk from the game, replacing any chunk at that position. {@code data} has one
     * boolean per block, indexed {@code y << 8 | z << 4 | x}, 16 * 16 * 256 of them (384 in the overworld).
     */
    public void insertChunkData(int chunkX, int chunkZ, boolean[] data) {
        final int blocksInChunk = 16 * 16 * dimensionHeight(this.dimension);
        if (data.length != blocksInChunk) {
            throw new IllegalArgumentException(this.dimension == Dimension.OVERWORLD ? "input is not 16 * 16 * 384 elements" : "input is not 16 * 16 * 256 elements");
        }
        final Chunk chunk = new Chunk();
        for (int i = 0; i < blocksInChunk; i++) {
            if (data[i]) {
                chunk.setBlock(i & 0xF, i >> 8, (i >> 4) & 0xF, true);
            }
        }
        this.chunks.put(key(chunkX, chunkZ), new Entry(STATE_FROM_CALLER, chunk, chunkX, chunkZ));
    }

    /** Inserts a new, empty chunk from the game at (x, z), replacing any chunk there, and returns it to be filled. */
    public Chunk allocateAndInsertChunk(int x, int z) {
        final Chunk chunk = new Chunk();
        this.chunks.put(key(x, z), new Entry(STATE_FROM_CALLER, chunk, x, z));
        return chunk;
    }

    /** The chunk at (x, z), or the shared all-solid or all-air chunk if there is none. Do not write to the shared ones. */
    public Chunk getChunkOrDefault(int x, int z, boolean solid) {
        final Entry e = this.chunks.get(key(x, z));
        return e != null ? e.chunk : (solid ? Chunk.SOLID : Chunk.AIR);
    }

    /** The chunk at (x, z), or null. */
    public Chunk getChunk(int x, int z) {
        final Entry e = this.chunks.get(key(x, z));
        return e != null ? e.chunk : null;
    }

    /** Marks the chunk at (x, z) as from the game or as made up. Returns true if the chunk existed and the change was made. */
    public boolean setChunkState(int x, int z, boolean fromCaller) {
        final Entry e = this.chunks.get(key(x, z));
        if (e == null) {
            return false;
        }
        e.state = fromCaller ? STATE_FROM_CALLER : STATE_FAKE;
        return true;
    }

    public boolean hasChunkFromCaller(int x, int z) {
        final Entry e = this.chunks.get(key(x, z));
        return e != null && e.state == STATE_FROM_CALLER;
    }

    /** Whether the table holds chunk (x, z), whichever way it got there. */
    boolean hasChunk(int x, int z) {
        return this.chunks.containsKey(key(x, z));
    }

    /** Forgets every chunk more than maxDistanceBlocks (in whole chunks) from (chunkX, chunkZ). */
    public void cullFarChunks(int chunkX, int chunkZ, int maxDistanceBlocks) {
        final long distChunks = maxDistanceBlocks / 16;
        final long distSq = distChunks * distChunks;
        this.chunks.values().removeIf(entry -> {
            final long dx = entry.x - chunkX;
            final long dz = entry.z - chunkZ;
            return dx * dx + dz * dz > distSq;
        });
    }

    /** How many chunks the table holds. */
    public int chunkCount() {
        return this.chunks.size();
    }

    // ---- searching ----

    /**
     * A path from (x1, y1, z1) towards (x2, y2, z2), or null if none was found in time or the
     * search was cancelled. The segment is finished if it reached the goal.
     *
     * @param atLeastX4               do not squeeze through cubes smaller than 4 blocks
     * @param refine                  drop points that are visible from an earlier one
     * @param failTimeoutInMillis     give up after this long with nothing; 0 for 30 seconds
     * @param defaultAirElseGenerate  treat unknown chunks as air, instead of generating them from the seed
     * @param fakeChunkCost           the cost of a step through a chunk that was not given by the game, against 1 for one that was
     */
    public PathSegment pathFind(int x1, int y1, int z1, int x2, int y2, int z2, boolean atLeastX4, boolean refine, int failTimeoutInMillis, boolean defaultAirElseGenerate, double fakeChunkCost) {
        if (!inBounds(y1) || !inBounds(y2)) {
            throw new IllegalArgumentException("Invalid y1 or y2");
        }
        // The flag is cleared when the search ends, not when one starts. Clearing it here would
        // drop a cancel that arrived while this call was still queued behind the lock -- which is
        // exactly the cancel that destroy() sends -- and that search would then run to its full
        // timeout holding the lock. Clearing on the way out instead means a cancellation is
        // honoured by the search it was aimed at and never leaks into the search after it.
        try {
            final Size size = atLeastX4 ? Size.X4 : Size.X2;
            final NodePos start = PathFinder.findAir(this, size, new BlockPos(x1, y1, z1), defaultAirElseGenerate);
            final NodePos goal = PathFinder.findAir(this, size, new BlockPos(x2, y2, z2), defaultAirElseGenerate);
            final PathFinder.Path path = PathFinder.findPathSegment(this, start, goal, atLeastX4, failTimeoutInMillis, defaultAirElseGenerate, fakeChunkCost);
            if (path == null) {
                return null;
            }
            return new PathSegment(path.type == PathFinder.Path.Type.FINISHED, refine ? Raytracer.refine(this, path.blocks) : path.blocks);
        } finally {
            this.cancelFlag.set(false);
        }
    }

    /** Asks a running search to stop. Returns whether it had already been asked. */
    public boolean cancel() {
        return this.cancelFlag.getAndSet(true);
    }

    boolean isCancelled() {
        return this.cancelFlag.get();
    }

    boolean clearCancelled() {
        return this.cancelFlag.getAndSet(false);
    }

    // ---- for the search and the rays ----

    Entry getChunkOrAir(int cx, int cz) {
        final Entry e = this.chunks.get(key(cx, cz));
        return e != null ? e : AIR_ENTRY;
    }

    Chunk getRealChunkOrDefault(int cx, int cz, boolean solid) {
        final Entry e = this.chunks.get(key(cx, cz));
        if (e == null || e.state != STATE_FROM_CALLER) {
            return solid ? Chunk.SOLID : Chunk.AIR;
        }
        return e.chunk;
    }

    /** The chunk at (cx, cz), generated from the seed and inserted if there is none. */
    Chunk getOrGenChunk(int cx, int cz) {
        final long key = key(cx, cz);
        final Entry e = this.chunks.get(key);
        if (e != null) {
            return e.chunk;
        }
        final Chunk chunk = this.generator.generateChunk(cx, cz);
        final Entry previous = this.chunks.putIfAbsent(key, new Entry(STATE_FAKE, chunk, cx, cz));
        // someone else generated this chunk while we were generating it
        return previous != null ? previous.chunk : chunk;
    }

    Chunk getRealChunkFromCacheOrFakeChunkMaybeGen(int cx, int cz, CacheMiss fakeChunkMode) {
        if (fakeChunkMode == CacheMiss.GENERATE) {
            return getOrGenChunk(cx, cz);
        }
        return getRealChunkOrDefault(cx, cz, fakeChunkMode == CacheMiss.SOLID);
    }

    /**
     * Loads the Baritone region holding chunk (cx, cz) the first time it is asked about. A chunk
     * that is already in the table wins over the file's. Returns the milliseconds spent on the file.
     */
    long tryLoadRegion(int cx, int cz) {
        if (this.baritoneCache == null) {
            return 0;
        }
        final int regionX = cx >> 5;
        final int regionZ = cz >> 5;
        if (!this.checkedRegions.add(key(regionX, regionZ))) {
            return 0;
        }
        final long t1 = System.currentTimeMillis();
        final boolean read = BaritoneRegion.load(this.baritoneCache, regionX, regionZ,
                (x, z, chunk) -> this.chunks.putIfAbsent(key(x, z), new Entry(STATE_FROM_CALLER, chunk, x, z)));
        return read ? System.currentTimeMillis() - t1 : 0;
    }
}
