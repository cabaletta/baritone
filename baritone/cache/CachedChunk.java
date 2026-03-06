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

import baritone.api.utils.BlockUtils;
import baritone.utils.pathing.PathingBlockType;
import baritone.api.utils.BlockPos; // assume ported

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.BitSet;

/**
 * @author Brady
 * @since 8/3/2018
 */
public final class CachedChunk {

    public static final java.util.Set<Block> BLOCKS_TO_KEEP_TRACK_OF = java.util.Set.of(
            Blocks.enderChest,
            Blocks.furnace,
            Blocks.chest,
            Blocks.trappedChest,
            Blocks.endPortal,
            Blocks.endPortalFrame,
            Blocks.mobSpawner,
            Blocks.barrier,
            Blocks.observer,
            Blocks.whiteShulkerBox,
            // add all shulker boxes if needed
            Blocks.portal,
            Blocks.hopper,
            Blocks.beacon,
            Blocks.brewingStand,
            Blocks.enchantingTable,
            Blocks.anvil,
            // beds
            Blocks.dragonEgg,
            Blocks.jukebox,
            Blocks.endGateway,
            Blocks.web,
            Blocks.netherWart,
            Blocks.ladder,
            Blocks.vine
    );

    public final int height;

    /**
     * The size of the chunk data in bits. Equal to 16 KiB.
     * <p>
     * Chunks are 16x16xH, each block requires 2 bits.
     */
    public final int size;

    /**
     * The size of the chunk data in bytes. Equal to 16 KiB for 256 height.
     */
    public final int sizeInBytes;

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

    private final Map<Integer, String> special;

    /**
     * The block names of each surface level block for generating an overview
     */
    private final Block[] overview;

    private final int[] heightMap;

    private final Map<String, List<BlockPos>> specialBlockLocations;

    public final long cacheTimestamp;

    CachedChunk(int x, int z, int height, BitSet data, Block[] overview, Map<String, List<BlockPos>> specialBlockLocations, long cacheTimestamp) {
        this.size = size(height);
        this.sizeInBytes = sizeInBytes(size);
        validateSize(data);

        this.x = x;
        this.z = z;
        this.height = height;
        this.data = data;
        this.overview = overview;
        this.heightMap = new int[256];
        this.specialBlockLocations = specialBlockLocations;
        this.cacheTimestamp = cacheTimestamp;
        if (specialBlockLocations.isEmpty()) {
            this.special = null;
        } else {
            this.special = new HashMap<>();
            setSpecial();
        }
        calculateHeightMap();
    }

    public static int size(int dimension_height) {
        return 2 * 16 * 16 * dimension_height;
    }

    public static int sizeInBytes(int size) {
        return size / 8;
    }

    private final void setSpecial() {
        for (Map.Entry<String, List<BlockPos>> entry : specialBlockLocations.entrySet()) {
            for (BlockPos pos : entry.getValue()) {
                special.put(getPositionIndex(pos.getX(), pos.getY(), pos.getZ()), entry.getKey());
            }
        }
    }

    public final Block getBlock(int x, int y, int z) {
        int index = getPositionIndex(x, y, z);
        PathingBlockType type = getType(index);
        int internalPos = z << 4 | x;
        if (heightMap[internalPos] == y && type != PathingBlockType.AVOID) {
            // if the top block in a column is water, we cache it as AVOID but we don't want to just return default state water (which is not flowing) beacuse then it would try to path through it

            // we have this exact block, it's a surface block
            return overview[internalPos];
        }
        if (special != null) {
            String str = special.get(index);
            if (str != null) {
                return BlockUtils.stringToBlock(str);
            }
        }

        if (type == PathingBlockType.SOLID) {
            if (y == 127 && dimension == -1) { // nether roof, adjust for 1.7.10
                return Blocks.bedrock;
            }
            if (y < 0) {
                return Blocks.obsidian;
            }
        }
        return ChunkPacker.pathingTypeToBlock(type);
    }

    private PathingBlockType getType(int index) {
        boolean bit0 = data.get(index);
        boolean bit1 = data.get(index + 1);
        if (!bit0 && !bit1) return PathingBlockType.AIR;
        if (bit0 && !bit1) return PathingBlockType.WATER;
        if (!bit0 && bit1) return PathingBlockType.AVOID;
        return PathingBlockType.SOLID;
    }

    private void calculateHeightMap() {
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int index = z << 4 | x;
                heightMap[index] = 0;
                for (int y = height; y >= 0; y--) {
                    int i = getPositionIndex(x, y, z);
                    if (data.get(i) || data.get(i + 1)) {
                        heightMap[index] = y;
                        break;
                    }
                }
            }
        }
    }

    public final Block[] getOverview() {
        return overview;
    }

    public final Map<String, List<BlockPos>> getRelativeBlocks() {
        return specialBlockLocations;
    }

    public final ArrayList<BlockPos> getAbsoluteBlocks(String blockType) {
        if (specialBlockLocations.get(blockType) == null) {
            return null;
        }
        ArrayList<BlockPos> res = new ArrayList<>();
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
     * packed chunk data. Sizes that exceed {@link CachedChunk#size} are
     * considered invalid, and thus, an exception will be thrown.
     *
     * @param data The raw data
     * @throws IllegalArgumentException if the bitset size exceeds the maximum size
     */
    private void validateSize(BitSet data) {
        if (data.size() > size) {
            throw new IllegalArgumentException("BitSet of invalid length provided");
        }
    }
}