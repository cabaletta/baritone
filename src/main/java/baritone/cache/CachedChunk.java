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

import baritone.api.cache.IBlockTypeAccess;
import baritone.utils.Helper;
import baritone.utils.pathing.PathingBlockType;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * @author Brady
 * @since 8/3/2018 1:04 AM
 */
public final class CachedChunk implements IBlockTypeAccess, Helper {

    public static final Set<Block> BLOCKS_TO_KEEP_TRACK_OF = Collections.unmodifiableSet(new HashSet<Block>() {{
        add(Blocks.DIAMOND_ORE);
        add(Blocks.DIAMOND_BLOCK);
        //add(Blocks.COAL_ORE);
        add(Blocks.COAL_BLOCK);
        //add(Blocks.IRON_ORE);
        add(Blocks.IRON_BLOCK);
        //add(Blocks.GOLD_ORE);
        add(Blocks.GOLD_BLOCK);
        add(Blocks.EMERALD_ORE);
        add(Blocks.EMERALD_BLOCK);

        add(Blocks.ENDER_CHEST);
        add(Blocks.FURNACE);
        add(Blocks.CHEST);
        add(Blocks.TRAPPED_CHEST);
        add(Blocks.END_PORTAL);
        add(Blocks.END_PORTAL_FRAME);
        add(Blocks.MOB_SPAWNER);
        // TODO add all shulker colors
        add(Blocks.PORTAL);
        add(Blocks.HOPPER);
        add(Blocks.BEACON);
        add(Blocks.BREWING_STAND);
        add(Blocks.SKULL);
        add(Blocks.ENCHANTING_TABLE);
        add(Blocks.ANVIL);
        add(Blocks.LIT_FURNACE);
        add(Blocks.BED);
        add(Blocks.DRAGON_EGG);
        add(Blocks.JUKEBOX);
        add(Blocks.END_GATEWAY);
        add(Blocks.WEB);
        add(Blocks.NETHER_WART);
    }});

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
    private final IBlockState[] overview;

    private final int[] heightMap;

    private final Map<String, List<BlockPos>> specialBlockLocations;

    public final long cacheTimestamp;

    CachedChunk(int x, int z, BitSet data, IBlockState[] overview, Map<String, List<BlockPos>> specialBlockLocations, long cacheTimestamp) {
        validateSize(data);

        this.x = x;
        this.z = z;
        this.data = data;
        this.overview = overview;
        this.heightMap = new int[256];
        this.specialBlockLocations = specialBlockLocations;
        this.cacheTimestamp = cacheTimestamp;
        calculateHeightMap();
    }

    @Override
    public final IBlockState getBlock(int x, int y, int z) {
        int internalPos = z << 4 | x;
        if (heightMap[internalPos] == y) {
            // we have this exact block, it's a surface block
            /*System.out.println("Saying that " + x + "," + y + "," + z + " is " + state);
            if (!Minecraft.getMinecraft().world.getBlockState(new BlockPos(x + this.x * 16, y, z + this.z * 16)).getBlock().equals(state.getBlock())) {
                throw new IllegalStateException("failed " + Minecraft.getMinecraft().world.getBlockState(new BlockPos(x + this.x * 16, y, z + this.z * 16)).getBlock() + " " + state.getBlock() + " " + (x + this.x * 16) + " " + y + " " + (z + this.z * 16));
            }*/
            return overview[internalPos];
        }
        PathingBlockType type = getType(x, y, z);
        if (type == PathingBlockType.SOLID && y == 127 && mc.player.dimension == -1) {
            return Blocks.BEDROCK.getDefaultState();
        }
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
                    int i = getPositionIndex(x, y, z);
                    if (data.get(i) || data.get(i + 1)) {
                        heightMap[index] = y;
                        break;
                    }
                }
            }
        }
    }

    public final IBlockState[] getOverview() {
        return overview;
    }

    public final Map<String, List<BlockPos>> getRelativeBlocks() {
        return specialBlockLocations;
    }

    public final LinkedList<BlockPos> getAbsoluteBlocks(String blockType) {
        if (specialBlockLocations.get(blockType) == null) {
            return null;
        }
        LinkedList<BlockPos> res = new LinkedList<>();
        for (BlockPos pos : specialBlockLocations.get(blockType)) {
            res.add(new BlockPos(pos.getX() + x * 16, pos.getY(), pos.getZ() + z * 16));
        }
        return res;
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
        return (x << 1) | (z << 5) | (y << 9);
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
        if (data.size() > SIZE) {
            throw new IllegalArgumentException("BitSet of invalid length provided");
        }
    }
}
