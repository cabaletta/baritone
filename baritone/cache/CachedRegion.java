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

package baritone.cache;

import baritone.api.cache.ICachedRegion;
import baritone.api.utils.BlockUtils;
import baritone.api.utils.BlockPos; // assume ported

import net.minecraft.block.Block;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Brady
 * @since 8/3/2018
 */
public final class CachedRegion implements ICachedRegion {

    private static final byte CHUNK_NOT_PRESENT = 0;
    private static final byte CHUNK_PRESENT = 1;

    /**
     * Magic value to detect invalid cache files, or incompatible cache files saved in an old version of Baritone
     */
    private static final int CACHED_REGION_MAGIC = 456022911;

    /**
     * All of the chunks in this region: A 32x32 array of them.
     */
    private final CachedChunk[][] chunks = new CachedChunk[32][32];

    /**
     * The region x coordinate
     */
    private final int x;

    /**
     * The region z coordinate
     */
    private final int z;

    /**
     * Has this region been modified since its most recent load or save
     */
    private boolean hasUnsavedChanges;

    CachedRegion(int x, int z) {
        this.x = x;
        this.z = z;
        this.hasUnsavedChanges = false;
    }

    @Override
    public final Block getBlock(int x, int y, int z) {
        CachedChunk chunk = chunks[x >> 4][z >> 4];
        if (chunk != null) {
            return chunk.getBlock(x & 15, y, z & 15);
        }
        return null;
    }

    @Override
    public final boolean isCached(int x, int z) {
        return chunks[x >> 4][z >> 4] != null;
    }

    public final ArrayList<BlockPos> getLocationsOf(String block) {
        ArrayList<BlockPos> res = new ArrayList<>();
        for (int chunkX = 0; chunkX < 32; chunkX++) {
            for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
                if (chunks[chunkX][chunkZ] == null) {
                    continue;
                }
                ArrayList<BlockPos> locs = chunks[chunkX][chunkZ].getAbsoluteBlocks(block);
                if (locs != null) {
                    res.addAll(locs);
                }
            }
        }
        return res;
    }

    public final synchronized void updateCachedChunk(int chunkX, int chunkZ, CachedChunk chunk) {
        this.chunks[chunkX][chunkZ] = chunk;
        hasUnsavedChanges = true;
    }

    public synchronized final void save(String directory) {
        if (!hasUnsavedChanges) {
            return;
        }
        removeExpired();
        try {
            Path path = Paths.get(directory);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            System.out.println("Saving region " + x + "," + z + " to disk " + path);
            Path regionFile = getRegionFile(path, this.x, this.z);
            if (!Files.exists(regionFile)) {
                Files.createFile(regionFile);
            }
            try (
                    FileOutputStream fileOut = new FileOutputStream(regionFile.toFile());
                    GZIPOutputStream gzipOut = new GZIPOutputStream(fileOut, 16384);
                    DataOutputStream out = new DataOutputStream(gzipOut)
            ) {
                out.writeInt(CACHED_REGION_MAGIC);
                for (int cx = 0; cx < 32; cx++) {
                    for (int cz = 0; cz < 32; cz++) {
                        CachedChunk chunk = this.chunks[cx][cz];
                        if (chunk == null) {
                            out.write(CHUNK_NOT_PRESENT);
                        } else {
                            out.write(CHUNK_PRESENT);
                            byte[] chunkBytes = chunk.toByteArray();
                            out.write(chunkBytes);
                            // Messy, but fills the empty 0s that should be trailing to fill up the space.
                            out.write(new byte[chunk.sizeInBytes - chunkBytes.length]);
                        }
                    }
                }
                for (int cx = 0; cx < 32; cx++) {
                    for (int cz = 0; cz < 32; cz++) {
                        if (chunks[cx][cz] != null) {
                            for (int i = 0; i < 256; i++) {
                                out.writeUTF(BlockUtils.blockToString(chunks[cx][cz].getOverview()[i]));
                            }
                        }
                    }
                }
                for (int cx = 0; cx < 32; cx++) {
                    for (int cz = 0; cz < 32; cz++) {
                        if (chunks[cx][cz] != null) {
                            Map<String, List<BlockPos>> locs = chunks[cx][cz].getRelativeBlocks();
                            out.writeShort(locs.entrySet().size());
                            for (Map.Entry<String, List<BlockPos>> entry : locs.entrySet()) {
                                out.writeUTF(entry.getKey());
                                out.writeShort(entry.getValue().size());
                                for (BlockPos pos : entry.getValue()) {
                                    out.writeByte((byte) (pos.getZ() << 4 | pos.getX()));
                                    out.writeInt(pos.getY());
                                }
                            }
                        }
                    }
                }
                for (int cx = 0; cx < 32; cx++) {
                    for (int cz = 0; cz < 32; cz++) {
                        if (chunks[cx][cz] != null) {
                            out.writeLong(chunks[cx][cz].cacheTimestamp);
                        }
                    }
                }
            }
            hasUnsavedChanges = false;
            System.out.println("Saved region successfully");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void load(String directory) {
        try {
            Path path = Paths.get(directory);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }

            Path regionFile = getRegionFile(path, this.x, this.z);
            if (!Files.exists(regionFile)) {
                return;
            }

            System.out.println("Loading region " + x + "," + z + " from disk " + path);
            long start = System.nanoTime() / 1000000L;

            try (
                    FileInputStream fileIn = new FileInputStream(regionFile.toFile());
                    GZIPInputStream gzipIn = new GZIPInputStream(fileIn, 32768);
                    DataInputStream in = new DataInputStream(gzipIn)
            ) {
                int magic = in.readInt();
                if (magic != CACHED_REGION_MAGIC) {
                    // in the future, if we change the format on disk
                    // we can keep converters for the old format
                    // by switching on the magic value, and either loading it normally, or loading through a converter.
                    throw new IOException("Bad magic value " + magic);
                }
                boolean[][] present = new boolean[32][32];
                BitSet[][] bitSets = new BitSet[32][32];
                Map<String, List<BlockPos>>[][] location = new HashMap[32][32];
                Block[][][] overview = new Block[32][32][256];
                long[][] cacheTimestamp = new long[32][32];
                for (int cx = 0; cx < 32; cx++) {
                    for (int cz = 0; cz < 32; cz++) {
                        int isChunkPresent = in.read();
                        switch (isChunkPresent) {
                            case CHUNK_PRESENT:
                                byte[] bytes = new byte[CachedChunk.sizeInBytes(CachedChunk.size(256))];
                                in.readFully(bytes);
                                bitSets[cx][cz] = BitSet.valueOf(bytes);
                                location[cx][cz] = new HashMap<>();
                                overview[cx][cz] = new Block[256];
                                present[cx][cz] = true;
                                break;
                            case CHUNK_NOT_PRESENT:
                                break;
                            default:
                                throw new IOException("Malformed stream");
                        }
                    }
                }
                for (int cx = 0; cx < 32; cx++) {
                    for (int cz = 0; cz < 32; cz++) {
                        if (present[cx][cz]) {
                            for (int i = 0; i < 256; i++) {
                                String name = in.readUTF();
                                overview[cx][cz][i] = BlockUtils.stringToBlock(name);
                            }
                        }
                    }
                }
                for (int cx = 0; cx < 32; cx++) {
                    for (int cz = 0; cz < 32; cz++) {
                        if (present[cx][cz]) {
                            // 16 * 16 * 256 = 65536 so a short is enough
                            // ^ haha jokes on leijurv, java doesn't have unsigned types so that isn't correct
                            //   also why would you have more than 32767 special blocks in a chunk
                            // haha double jokes on you now it works for 65535 not just 32767
                            int numSpecialBlockTypes = in.readShort() & 0xffff;
                            for (int i = 0; i < numSpecialBlockTypes; i++) {
                                String blockName = in.readUTF();
                                BlockUtils.stringToBlockRequired(blockName);
                                List<BlockPos> locs = new ArrayList<>();
                                location[cx][cz].put(blockName, locs);
                                int numLocations = in.readShort() & 0xffff;
                                if (numLocations == 0) {
                                    // an entire chunk full of air can happen in the end
                                    numLocations = 65536;
                                }
                                for (int j = 0; j < numLocations; j++) {
                                    byte xz = in.readByte();
                                    int X = xz & 0x0f;
                                    int Z = (xz >>> 4) & 0x0f;
                                    int Y = in.readInt();
                                    locs.add(new BlockPos(X, Y, Z));
                                }
                            }
                        }
                    }
                }
                for (int cx = 0; cx < 32; cx++) {
                    for (int cz = 0; cz < 32; cz++) {
                        if (present[cx][cz]) {
                            cacheTimestamp[cx][cz] = in.readLong();
                        }
                    }
                }
                // only if the entire file was uncorrupted do we actually set the chunks
                for (int cx = 0; cx < 32; cx++) {
                    for (int cz = 0; cz < 32; cz++) {
                        if (present[cx][cz]) {
                            int chunkX = cx + 32 * this.x;
                            int chunkZ = cz + 32 * this.z;
                            this.chunks[cx][cz] = new CachedChunk(chunkX, chunkZ, 256, bitSets[cx][cz], overview[cx][cz], location[cx][cz], cacheTimestamp[cx][cz]);
                        }
                    }
                }
            }
            removeExpired();
            hasUnsavedChanges = false;
            long end = System.nanoTime() / 1000000L;
            System.out.println("Loaded region successfully in " + (end - start) + "ms");
        } catch (Exception ex) { // corrupted files can cause NullPointerExceptions as well as IOExceptions
            ex.printStackTrace();
        }
    }

    public synchronized final void removeExpired() {
        long expiry = Baritone.settings().cachedChunksExpirySeconds.value;
        if (expiry < 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long oldestAcceptableAge = now - expiry * 1000L;
        for (int cx = 0; cx < 32; cx++) {
            for (int cz = 0; cz < 32; cz++) {
                if (this.chunks[cx][cz] != null && this.chunks[cx][cz].cacheTimestamp < oldestAcceptableAge) {
                    System.out.println("Removing chunk " + (cx + 32 * this.x) + "," + (cz + 32 * this.z) + " because it was cached " + (now - this.chunks[cx][cz].cacheTimestamp) / 1000L + " seconds ago, and max age is " + expiry);
                    this.chunks[cx][cz] = null;
                }
            }
        }
    }

    public synchronized final CachedChunk mostRecentlyModified() {
        CachedChunk recent = null;
        for (int cx = 0; cx < 32; cx++) {
            for (int cz = 0; cz < 32; cz++) {
                if (this.chunks[cx][cz] == null) {
                    continue;
                }
                if (recent == null || this.chunks[cx][cz].cacheTimestamp > recent.cacheTimestamp) {
                    recent = this.chunks[cx][cz];
                }
            }
        }
        return recent;
    }

    /**
     * @return The region x coordinate
     */
    @Override
    public final int getX() {
        return this.x;
    }

    /**
     * @return The region z coordinate
     */
    @Override
    public final int getZ() {
        return this.z;
    }

    private static Path getRegionFile(Path cacheDir, int regionX, int regionZ) {
        return Paths.get(cacheDir.toString(), "r." + regionX + "." + regionZ + ".bcr");
    }
}