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
import baritone.pathing.movement.MovementHelper;
import baritone.utils.pathing.PathingBlockType;
import baritone.api.utils.BlockPos; // assume ported

import net.minecraft.block.Block;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.init.Blocks;

import java.util.*;

/**
 * @author Brady
 * @since 8/3/2018
 */
public final class ChunkPacker {

    private ChunkPacker() {}

    public static CachedChunk pack(Chunk chunk) {
        //long start = System.nanoTime() / 1000000L;

        Map<String, List<BlockPos>> specialBlocks = new HashMap<>();
        final int height = 256;
        BitSet bitSet = new BitSet(CachedChunk.size(height));
        ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
        for (int ySection = 0; ySection < sections.length; ySection++) {
            ExtendedBlockStorage section = sections[ySection];
            if (section == null) {
                continue;
            }
            int yBase = ySection << 4;
            for (int y = 0; y < 16; y++) {
                int worldY = yBase + y;
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        int id = section.getExtBlockID(x, y, z);
                        int meta = section.getExtMetadata(x, y, z);
                        if (id == 0) continue;
                        int index = CachedChunk.getPositionIndex(x, worldY, z);
                        PathingBlockType type = getPathingBlockType(id, meta, chunk, x, worldY, z);
                        boolean[] bits = type.getBits();
                        bitSet.set(index, bits[0]);
                        bitSet.set(index + 1, bits[1]);
                        Block block = Block.getBlockById(id);
                        if (BLOCKS_TO_KEEP_TRACK_OF.contains(block)) {
                            String name = block.getUnlocalizedName();
                            specialBlocks.computeIfAbsent(name, b -> new ArrayList<>()).add(new BlockPos(x, worldY, z));
                        }
                    }
                }
            }
        }
        //long end = System.nanoTime() / 1000000L;
        //System.out.println("Chunk packing took " + (end - start) + "ms for " + chunk.xPosition + "," + chunk.zPosition);
        Block[] blocks = new Block[256];

        // get top block in columns
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                Block top = null;
                for (int y = height - 1; y >= 0; y--) {
                    int sectionY = y >> 4;
                    int localY = y & 15;
                    ExtendedBlockStorage section = sections[sectionY];
                    if (section == null) continue;
                    int id = section.getExtBlockID(x, localY, z);
                    if (id != 0) {
                        top = Block.getBlockById(id);
                        break;
                    }
                }
                blocks[z << 4 | x] = top != null ? top : Blocks.air;
            }
        }
        return new CachedChunk(chunk.xPosition, chunk.zPosition, height, bitSet, blocks, specialBlocks, System.currentTimeMillis());
    }

    private static PathingBlockType getPathingBlockType(int id, int meta, Chunk chunk, int x, int y, int z) {
        if (id == 0) return PathingBlockType.AIR;
        Block block = Block.getBlockById(id);

        // LOTR compatibility module
        String className = block.getClass().getName();
        if (className.contains("lotr") || className.contains("LOTR")) {
            String unloc = block.getUnlocalizedName();
            if (unloc.contains("stone") || unloc.contains("rock") || unloc.contains("brick") || unloc.contains("cobblestone")) {
                return PathingBlockType.SOLID;
            } else if (unloc.contains("path") || unloc.contains("road") || unloc.contains("floor") || unloc.contains("plank")) {
                return PathingBlockType.AIR;
            } else if (unloc.contains("grass") || unloc.contains("dirt") || unloc.contains("sand")) {
                return PathingBlockType.AIR;
            } else if (unloc.contains("wood") || unloc.contains("log") || unloc.contains("planks")) {
                return PathingBlockType.SOLID;
            } else if (unloc.contains("water") || unloc.contains("lava")) {
                return PathingBlockType.AVOID;
            }
            // default to solid for unknown LOTR blocks to avoid unexpected falls
            return PathingBlockType.SOLID;
        }

        // vanilla logic
        if (block == Blocks.water) {
            if (meta > 0) {
                return PathingBlockType.AVOID;
            }
            return PathingBlockType.WATER;
        } else if (block == Blocks.lava) {
            return PathingBlockType.AVOID;
        } else if (block.isPassable(chunk.worldObj, new net.minecraft.util.BlockPos(x + chunk.xPosition * 16, y, z + chunk.zPosition * 16))) {
            return PathingBlockType.AIR;
        } else if (block.isOpaqueCube()) {
            return PathingBlockType.SOLID;
        } else {
            return PathingBlockType.AVOID;
        }
    }

    public static Block pathingTypeToBlock(PathingBlockType type) {
        switch (type) {
            case AIR:
                return Blocks.air;
            case WATER:
                return Blocks.water;
            case AVOID:
                return Blocks.lava;
            case SOLID:
                return Blocks.stone;
            default:
                return null;
        }
    }
}