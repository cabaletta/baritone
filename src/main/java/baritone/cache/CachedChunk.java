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

import baritone.utils.pathing.PathingBlockType;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * @author Brady
 * @since 8/3/2018
 */
public final class CachedChunk {

    public static final Set<Block> BLOCKS_TO_KEEP_TRACK_OF;

    static {
        HashSet<Block> temp = new HashSet<>();
        //temp.add(Blocks.DIAMOND_ORE);
        temp.add(Blocks.DIAMOND_BLOCK);
        //temp.add(Blocks.COAL_ORE);
        temp.add(Blocks.COAL_BLOCK);
        //temp.add(Blocks.IRON_ORE);
        temp.add(Blocks.IRON_BLOCK);
        //temp.add(Blocks.GOLD_ORE);
        temp.add(Blocks.GOLD_BLOCK);
        temp.add(Blocks.EMERALD_ORE);
        temp.add(Blocks.EMERALD_BLOCK);

        temp.add(Blocks.ENDER_CHEST);
        temp.add(Blocks.FURNACE);
        temp.add(Blocks.CHEST);
        temp.add(Blocks.TRAPPED_CHEST);
        temp.add(Blocks.END_PORTAL);
        temp.add(Blocks.END_PORTAL_FRAME);
        temp.add(Blocks.MOB_SPAWNER);
        temp.add(Blocks.BARRIER);
        temp.add(Blocks.OBSERVER);
        temp.add(Blocks.WHITE_SHULKER_BOX);
        temp.add(Blocks.ORANGE_SHULKER_BOX);
        temp.add(Blocks.MAGENTA_SHULKER_BOX);
        temp.add(Blocks.LIGHT_BLUE_SHULKER_BOX);
        temp.add(Blocks.YELLOW_SHULKER_BOX);
        temp.add(Blocks.LIME_SHULKER_BOX);
        temp.add(Blocks.PINK_SHULKER_BOX);
        temp.add(Blocks.GRAY_SHULKER_BOX);
        temp.add(Blocks.SILVER_SHULKER_BOX);
        temp.add(Blocks.CYAN_SHULKER_BOX);
        temp.add(Blocks.PURPLE_SHULKER_BOX);
        temp.add(Blocks.BLUE_SHULKER_BOX);
        temp.add(Blocks.BROWN_SHULKER_BOX);
        temp.add(Blocks.GREEN_SHULKER_BOX);
        temp.add(Blocks.RED_SHULKER_BOX);
        temp.add(Blocks.BLACK_SHULKER_BOX);
        temp.add(Blocks.PORTAL);
        temp.add(Blocks.HOPPER);
        temp.add(Blocks.BEACON);
        temp.add(Blocks.BREWING_STAND);
        temp.add(Blocks.SKULL);
        temp.add(Blocks.ENCHANTING_TABLE);
        temp.add(Blocks.ANVIL);
        temp.add(Blocks.LIT_FURNACE);
        temp.add(Blocks.BED);
        temp.add(Blocks.DRAGON_EGG);
        temp.add(Blocks.JUKEBOX);
        temp.add(Blocks.END_GATEWAY);
        temp.add(Blocks.WEB);
        temp.add(Blocks.NETHER_WART);
        temp.add(Blocks.LADDER);
        temp.add(Blocks.VINE);
        BLOCKS_TO_KEEP_TRACK_OF = Collections.unmodifiableSet(temp);
    }

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

    private final BitSet special;

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
        this.special = new BitSet();
        calculateHeightMap();
        setSpecial();
    }

    private final void setSpecial() {
        for (List<BlockPos> list : specialBlockLocations.values()) {
            for (BlockPos pos : list) {
                System.out.println("Turning on bit");
                special.set(getPositionIndex(pos.getX(), pos.getY(), pos.getZ()) >> 1);
            }
        }
    }

    public final IBlockState getBlock(int x, int y, int z, int dimension) {
        int index = getPositionIndex(x, y, z);
        PathingBlockType type = getType(index);
        int internalPos = z << 4 | x;
        if (heightMap[internalPos] == y && type != PathingBlockType.AVOID) {
            // if the top block in a column is water, we cache it as AVOID but we don't want to just return default state water (which is not flowing) beacuse then it would try to path through it

            // we have this exact block, it's a surface block
            /*System.out.println("Saying that " + x + "," + y + "," + z + " is " + state);
            if (!Minecraft.getMinecraft().world.getBlockState(new BlockPos(x + this.x * 16, y, z + this.z * 16)).getBlock().equals(state.getBlock())) {
                throw new IllegalStateException("failed " + Minecraft.getMinecraft().world.getBlockState(new BlockPos(x + this.x * 16, y, z + this.z * 16)).getBlock() + " " + state.getBlock() + " " + (x + this.x * 16) + " " + y + " " + (z + this.z * 16));
            }*/
            return overview[internalPos];
        }
        if (special.get(index >> 1)) {
            // this block is special
            for (Map.Entry<String, List<BlockPos>> entry : specialBlockLocations.entrySet()) {
                for (BlockPos pos : entry.getValue()) {
                    if (pos.getX() == x && pos.getY() == y && pos.getZ() == z) {
                        return ChunkPacker.stringToBlock(entry.getKey()).getDefaultState();
                    }
                }
            }
        }

        if (type == PathingBlockType.SOLID && y == 127 && dimension == -1) {
            return Blocks.BEDROCK.getDefaultState();
        }
        return ChunkPacker.pathingTypeToBlock(type, dimension);
    }

    private PathingBlockType getType(int index) {
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
