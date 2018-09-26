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
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Brady
 * @since 8/3/2018 9:35 PM
 */
public final class CachedRegion implements ICachedRegion {

    private static final byte CHUNK_NOT_PRESENT = 0;
    private static final byte CHUNK_PRESENT = 1;

    /**
     * Magic value to detect invalid cache files, or incompatible cache files saved in an old version of Baritone
     */
    private static final int CACHED_REGION_MAGIC = 456022910;

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
    public final IBlockState getBlock(int x, int y, int z) {
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

    public final LinkedList<BlockPos> getLocationsOf(String block) {
        LinkedList<BlockPos> res = new LinkedList<>();
        for (int chunkX = 0; chunkX < 32; chunkX++) {
            for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
                if (chunks[chunkX][chunkZ] == null) {
                    continue;
                }
                List<BlockPos> locs = chunks[chunkX][chunkZ].getAbsoluteBlocks(block);
                if (locs == null) {
                    continue;
                }
                for (BlockPos pos : locs) {
                    res.add(pos);
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
                for (int z = 0; z < 32; z++) {
                    for (int x = 0; x < 32; x++) {
                        CachedChunk chunk = this.chunks[x][z];
                        if (chunk == null) {
                            out.write(CHUNK_NOT_PRESENT);
                        } else {
                            out.write(CHUNK_PRESENT);
                            byte[] chunkBytes = chunk.toByteArray();
                            out.write(chunkBytes);
                            // Messy, but fills the empty 0s that should be trailing to fill up the space.
                            out.write(new byte[CachedChunk.SIZE_IN_BYTES - chunkBytes.length]);
                        }
                    }
                }
                for (int z = 0; z < 32; z++) {
                    for (int x = 0; x < 32; x++) {
                        if (chunks[x][z] != null) {
                            for (int i = 0; i < 256; i++) {
                                out.writeUTF(ChunkPacker.blockToString(chunks[x][z].getOverview()[i].getBlock()));
                            }
                        }
                    }
                }
                for (int z = 0; z < 32; z++) {
                    for (int x = 0; x < 32; x++) {
                        if (chunks[x][z] != null) {
                            Map<String, List<BlockPos>> locs = chunks[x][z].getRelativeBlocks();
                            out.writeShort(locs.entrySet().size());
                            for (Map.Entry<String, List<BlockPos>> entry : locs.entrySet()) {
                                out.writeUTF(entry.getKey());
                                out.writeShort(entry.getValue().size());
                                for (BlockPos pos : entry.getValue()) {
                                    out.writeByte((byte) (pos.getZ() << 4 | pos.getX()));
                                    out.writeByte((byte) (pos.getY()));
                                }
                            }
                        }
                    }
                }
            }
            hasUnsavedChanges = false;
            System.out.println("Saved region successfully");
        } catch (IOException ex) {
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
                CachedChunk[][] tmpCached = new CachedChunk[32][32];
                Map<String, List<BlockPos>>[][] location = new Map[32][32];
                for (int z = 0; z < 32; z++) {
                    for (int x = 0; x < 32; x++) {
                        int isChunkPresent = in.read();
                        switch (isChunkPresent) {
                            case CHUNK_PRESENT:
                                byte[] bytes = new byte[CachedChunk.SIZE_IN_BYTES];
                                in.readFully(bytes);
                                location[x][z] = new HashMap<>();
                                int regionX = this.x;
                                int regionZ = this.z;
                                int chunkX = x + 32 * regionX;
                                int chunkZ = z + 32 * regionZ;
                                tmpCached[x][z] = new CachedChunk(chunkX, chunkZ, BitSet.valueOf(bytes), new IBlockState[256], location[x][z]);
                                break;
                            case CHUNK_NOT_PRESENT:
                                tmpCached[x][z] = null;
                                break;
                            default:
                                throw new IOException("Malformed stream");
                        }
                    }
                }
                for (int z = 0; z < 32; z++) {
                    for (int x = 0; x < 32; x++) {
                        if (tmpCached[x][z] != null) {
                            for (int i = 0; i < 256; i++) {
                                tmpCached[x][z].getOverview()[i] = ChunkPacker.stringToBlock(in.readUTF()).getDefaultState();
                            }
                        }
                    }
                }
                for (int z = 0; z < 32; z++) {
                    for (int x = 0; x < 32; x++) {
                        if (tmpCached[x][z] != null) {
                            // 16 * 16 * 256 = 65536 so a short is enough
                            // ^ haha jokes on leijurv, java doesn't have unsigned types so that isn't correct
                            //   also why would you have more than 32767 special blocks in a chunk
                            // haha double jokes on you now it works for 65535 not just 32767
                            int numSpecialBlockTypes = in.readShort() & 0xffff;
                            for (int i = 0; i < numSpecialBlockTypes; i++) {
                                String blockName = in.readUTF();
                                List<BlockPos> locs = new ArrayList<>();
                                location[x][z].put(blockName, locs);
                                int numLocations = in.readShort() & 0xffff;
                                if (numLocations == 0) {
                                    // an entire chunk full of air can happen in the end
                                    numLocations = 65536;
                                }
                                for (int j = 0; j < numLocations; j++) {
                                    byte xz = in.readByte();
                                    int X = xz & 0x0f;
                                    int Z = (xz >>> 4) & 0x0f;
                                    int Y = in.readByte() & 0xff;
                                    locs.add(new BlockPos(X, Y, Z));
                                }
                            }
                        }
                    }
                }
                // only if the entire file was uncorrupted do we actually set the chunks
                for (int x = 0; x < 32; x++) {
                    for (int z = 0; z < 32; z++) {
                        this.chunks[x][z] = tmpCached[x][z];
                    }
                }
            }
            hasUnsavedChanges = false;
            long end = System.nanoTime() / 1000000L;
            System.out.println("Loaded region successfully in " + (end - start) + "ms");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
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
