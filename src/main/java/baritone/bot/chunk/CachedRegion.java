package baritone.bot.chunk;

import baritone.bot.pathing.util.PathingBlockType;
import baritone.bot.utils.GZIPUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.BitSet;
import java.util.zip.GZIPOutputStream;

/**
 * @author Brady
 * @since 8/3/2018 9:35 PM
 */
public final class CachedRegion implements ICachedChunkAccess {

    /**
     * All of the chunks in this region. A 16x16 array of them.
     * <p>
     * I would make these 32x32 regions to be in line with the Anvil format, but 16 is a nice number.
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

    CachedRegion(int x, int z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public final PathingBlockType getBlockType(int x, int y, int z) {
        CachedChunk chunk = this.getChunk(x >> 4, z >> 4);
        if (chunk != null) {
            return chunk.getBlockType(x, y, z);
        }
        return null;
    }

    @Override
    public final void updateCachedChunk(int chunkX, int chunkZ, BitSet data) {
        CachedChunk chunk = this.getChunk(chunkX, chunkZ);
        if (chunk == null) {
            this.chunks[chunkX][chunkZ] = new CachedChunk(chunkX, chunkZ, data);
        } else {
            chunk.updateContents(data);
        }
    }

    private CachedChunk getChunk(int chunkX, int chunkZ) {
        return this.chunks[chunkX][chunkZ];
    }

    public final void save(String directory) {
        try {
            Path path = Paths.get(directory);
            if (!Files.exists(path))
                Files.createDirectories(path);

            Path regionFile = getRegionFile(path, this.x, this.z);
            if (!Files.exists(regionFile))
                Files.createFile(regionFile);
            try (FileOutputStream fileOut = new FileOutputStream(regionFile.toFile()); GZIPOutputStream out = new GZIPOutputStream(fileOut)) {
                for (int z = 0; z < 32; z++) {
                    for (int x = 0; x < 32; x++) {
                        CachedChunk chunk = this.chunks[x][z];
                        if (chunk == null) {
                            out.write(CachedChunk.EMPTY_CHUNK);
                        } else {
                            byte[] chunkBytes = chunk.toByteArray();
                            out.write(chunkBytes);
                            // Messy, but fills the empty 0s that should be trailing to fill up the space.
                            out.write(new byte[CachedChunk.SIZE_IN_BYTES - chunkBytes.length]);
                        }
                    }
                }
            }
        } catch (IOException ignored) {}
    }

    public void load(String directory) {
        try {
            Path path = Paths.get(directory);
            if (!Files.exists(path))
                Files.createDirectories(path);

            Path regionFile = getRegionFile(path, this.x, this.z);
            if (!Files.exists(regionFile))
                return;

            byte[] decompressed;
            try (FileInputStream in = new FileInputStream(regionFile.toFile())) {
                decompressed = GZIPUtils.decompress(in);
            }

            if (decompressed == null)
                return;

            for (int z = 0; z < 32; z++) {
                for (int x = 0; x < 32; x++) {
                    int index = (x + (z << 5)) * CachedChunk.SIZE_IN_BYTES;
                    byte[] bytes = Arrays.copyOfRange(decompressed, index, index + CachedChunk.SIZE_IN_BYTES);
                    if (isAllZeros(bytes)) {
                        this.chunks[x][z] = null;
                    } else {
                        BitSet bits = BitSet.valueOf(bytes);
                        updateCachedChunk(x, z, bits);
                    }
                }
            }
        } catch (IOException ignored) {}
    }

    private static boolean isAllZeros(final byte[] array) {
        for (byte b : array) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return The region x coordinate
     */
    public final int getX() {
        return this.x;
    }

    /**
     * @return The region z coordinate
     */
    public final int getZ() {
        return this.z;
    }

    private static Path getRegionFile(Path cacheDir, int regionX, int regionZ) {
        return Paths.get(cacheDir.toString(), "r." + regionX + "." + regionZ + ".bcr");
    }
}
