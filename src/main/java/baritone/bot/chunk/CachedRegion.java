package baritone.bot.chunk;

import baritone.bot.pathing.util.PathingBlockType;
import baritone.bot.utils.GZIPUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.BitSet;

/**
 * @author Brady
 * @since 8/3/2018 9:35 PM
 */
public final class CachedRegion implements ICachedChunkAccess {

    /**
     * All of the chunks in this region. A 16x16 array of them.
     *
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
        if (chunk == null)
            this.chunks[chunkX][chunkZ] = new CachedChunk(chunkX, chunkZ, data);
        else
            chunk.updateContents(data);
    }

    private CachedChunk getChunk(int chunkX, int chunkZ) {
        return this.chunks[chunkX][chunkZ];
    }

    public final void save(String directory) {
        try {
            Path path = Paths.get(directory);
            if (!Files.exists(path))
                Files.createDirectories(path);

            ByteArrayOutputStream bos = new ByteArrayOutputStream(32 * 32 * CachedChunk.SIZE_IN_BYTES);
            for (int z = 0; z < 32; z++) {
                for (int x = 0; x < 32; x++) {
                    CachedChunk chunk = this.chunks[x][z];
                    if (chunk == null) {
                        bos.write(CachedChunk.EMPTY_CHUNK);
                    } else {
                        byte[] chunkBytes = chunk.toByteArray();
                        bos.write(chunkBytes);
                        // Messy, but fills the empty 0s that should be trailing to fill up the space.
                        bos.write(new byte[CachedChunk.SIZE_IN_BYTES - chunkBytes.length]);
                    }
                }
            }

            Path regionFile = getRegionFile(path, this.x, this.z);
            if (!Files.exists(regionFile))
                Files.createFile(regionFile);

            byte[] compressed = GZIPUtils.compress(bos.toByteArray());
            if (compressed != null)
                Files.write(regionFile, compressed);
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

            byte[] fileBytes = Files.readAllBytes(regionFile);
            byte[] decompressed = GZIPUtils.decompress(fileBytes);
            if (decompressed == null)
                return;

            for (int z = 0; z < 32; z++) {
                for (int x = 0; x < 32; x++) {
                    CachedChunk chunk = this.chunks[x][z];
                    if (chunk != null) {
                        int index = (x + (z << 5)) * CachedChunk.SIZE_IN_BYTES;
                        byte[] bytes = Arrays.copyOfRange(decompressed, index, index + CachedChunk.SIZE_IN_BYTES);
                        BitSet bits = BitSet.valueOf(bytes);
                        chunk.updateContents(bits);
                    }
                }
            }
        } catch (IOException ignored) {}
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
        return Paths.get(cacheDir.toString() + "\\r." + regionX + "." + regionZ + ".bcr");
    }
}
