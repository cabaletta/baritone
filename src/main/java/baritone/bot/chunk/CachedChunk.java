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
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

import java.util.BitSet;

/**
 * @author Brady
 * @since 8/3/2018 1:04 AM
 */
public final class CachedChunk implements IBlockTypeAccess {

    /**
     * The size of the chunk data in bits. Equal to 16 KiB.
     * <p>
     * Chunks are 16x16x256, each block requires 2 bits.
     */
    public static final int SIZE = 2 * 16 * 16 * 256;

    /**
     * The size of the chunk data in bytes. Equal to 16 KiB.
     */
    public static final int SIZE_IN_BYTES = SIZE / 8;

    /**
     * The chunk x coordinate
     */
    public final int x;

    /**
     * The chunk z coordinate
     */
    public final int z;

    /**
     * The actual raw data of this packed chunk.
     * <p>
     * Each block is expressed as 2 bits giving a total of 16 KiB
     */
    private final BitSet data;

    /**
     * The block names of each surface level block for generating an overview
     */
    private final String[] overview;

    private final int[] heightMap;

    CachedChunk(int x, int z, BitSet data, String[] overview) {
        validateSize(data);

        this.x = x;
        this.z = z;
        this.data = data;
        this.overview = overview;
        this.heightMap = new int[256];
        calculateHeightMap();
    }

    @Override
    public final IBlockState getBlock(int x, int y, int z) {
        int internalPos = z << 4 | x;
        if (heightMap[internalPos] == y) {
            // we have this exact block, it's a surface block
            String name = overview[internalPos];
            if (!name.contains(":")) {
                name = "minecraft:" + name;
            }
            IBlockState state = Block.getBlockFromName(name).getDefaultState();
            System.out.println("Saying that " + x + "," + y + "," + z + " is " + state);
            return state;
        }
        PathingBlockType type = getType(x, y, z);
        return ChunkPacker.pathingTypeToBlock(type);
    }

    private PathingBlockType getType(int x, int y, int z) {
        int index = getPositionIndex(x, y, z);
        return PathingBlockType.fromBits(data.get(index), data.get(index + 1));
    }

    private void calculateHeightMap() {
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int index = z << 4 | x;
                heightMap[index] = 0;
                for (int y = 256; y >= 0; y--) {
                    if (getType(x, y, z) != PathingBlockType.AIR) {
                        heightMap[index] = y;
                        break;
                    }
                }
            }
        }
    }

    public final String[] getOverview() {
        return overview;
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

    /**
     * Validates the size of an input {@link BitSet} containing the raw
     * packed chunk data. Sizes that exceed {@link CachedChunk#SIZE} are
     * considered invalid, and thus, an exception will be thrown.
     *
     * @param data The raw data
     * @throws IllegalArgumentException if the bitset size exceeds the maximum size
     */
    private static void validateSize(BitSet data) {
        if (data.size() > SIZE)
            throw new IllegalArgumentException("BitSet of invalid length provided");
    }
}
