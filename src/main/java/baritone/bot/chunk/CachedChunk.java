package baritone.bot.chunk;

import baritone.bot.pathing.util.IBlockTypeAccess;
import baritone.bot.pathing.util.PathingBlockType;

import java.util.BitSet;

/**
 * @author Brady
 * @since 8/3/2018 1:04 AM
 */
public final class CachedChunk implements IBlockTypeAccess {

    /**
     * The size of the chunk data in bits. Equal to 16 KiB.
     * <br>
     * Chunks are 16x16x256, each block requires 2 bits.
     */
    public static final int SIZE = 2 * 16 * 16 * 256;

    /**
     * The size of the chunk data in bytes. Equal to 16 KiB.
     */
    public static final int SIZE_IN_BYTES = SIZE / 8;

    /**
     * An array of just 0s with the length of {@link CachedChunk#SIZE_IN_BYTES}
     */
    public static final byte[] EMPTY_CHUNK = new byte[SIZE_IN_BYTES];

    /**
     * The chunk x coordinate
     */
    private final int x;

    /**
     * The chunk z coordinate
     */
    private final int z;

    /**
     * The actual raw data of this packed chunk.
     * <br>
     * Each block is expressed as 2 bits giving a total of 16 KiB
     */
    private final BitSet data;

    CachedChunk(int x, int z, BitSet data) {
        if (data.size() != SIZE)
            throw new IllegalArgumentException("BitSet of invalid length provided");

        this.x = x;
        this.z = z;
        this.data = data;
    }

    @Override
    public final PathingBlockType getBlockType(int x, int y, int z) {
        int index = getPositionIndex(x, y, z);
        return PathingBlockType.fromBits(data.get(index), data.get(index + 1));
    }

    void updateContents(BitSet data) {
        if (data.size() > SIZE)
            throw new IllegalArgumentException("BitSet of invalid length provided");

        for (int i = 0; i < data.length(); i++)
            this.data.set(i, data.get(i));
    }

    /**
     * @return Thee chunk x coordinat
     */
    public final int getX() {
        return this.x;
    }

    /**
     * @return The chunk z coordinate
     */
    public final int getZ() {
        return this.z;
    }

    /**
     * @return Returns the raw packed chunk data as a byte array
     */
    public final byte[] toByteArray() {
        return this.data.toByteArray();
    }

    /**
     * Returns the raw bit index of the specified position
     *
     * @param x The x position
     * @param y The y position
     * @param z The z position
     * @return The bit index
     */
    public static int getPositionIndex(int x, int y, int z) {
        return (x + (z << 4) + (y << 8)) * 2;
    }
}
