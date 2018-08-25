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

package baritone.chunk;

import baritone.pathing.movement.MovementHelper;
import baritone.utils.BlockStateInterface;
import baritone.utils.Helper;
import baritone.utils.pathing.PathingBlockType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import java.util.*;

/**
 * @author Brady
 * @since 8/3/2018 1:09 AM
 */
public final class ChunkPacker implements Helper {

    private ChunkPacker() {}

    public static CachedChunk pack(Chunk chunk) {
        long start = System.currentTimeMillis();

        Map<String, List<BlockPos>> specialBlocks = new HashMap<>();
        BitSet bitSet = new BitSet(CachedChunk.SIZE);
        try {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        int index = CachedChunk.getPositionIndex(x, y, z);
                        Block block = chunk.getBlockState(x, y, z).getBlock();
                        boolean[] bits = getPathingBlockType(block).getBits();
                        bitSet.set(index, bits[0]);
                        bitSet.set(index + 1, bits[1]);
                        if (CachedChunk.BLOCKS_TO_KEEP_TRACK_OF.contains(block)) {
                            String name = blockToString(block);
                            specialBlocks.computeIfAbsent(name, b -> new ArrayList<>()).add(new BlockPos(x, y, z));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println("Packed special blocks: " + specialBlocks);
        long end = System.currentTimeMillis();
        //System.out.println("Chunk packing took " + (end - start) + "ms for " + chunk.x + "," + chunk.z);
        String[] blockNames = new String[256];
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int height = chunk.getHeightValue(x, z);
                IBlockState blockState = chunk.getBlockState(x, height, z);
                for (int y = height; y > 0; y--) {
                    blockState = chunk.getBlockState(x, y, z);
                    if (getPathingBlockType(blockState.getBlock()) != PathingBlockType.AIR) {
                        break;
                    }
                }
                String name = blockToString(blockState.getBlock());
                blockNames[z << 4 | x] = name;
            }
        }
        CachedChunk cached = new CachedChunk(chunk.x, chunk.z, bitSet, blockNames, specialBlocks);
        return cached;
    }

    public static String blockToString(Block block) {
        ResourceLocation loc = Block.REGISTRY.getNameForObject(block);
        String name = loc.getPath(); // normally, only write the part after the minecraft:
        if (!loc.getNamespace().equals("minecraft")) {
            // Baritone is running on top of forge with mods installed, perhaps?
            name = loc.toString(); // include the namespace with the colon
        }
        return name;
    }

    public static Block stringToBlock(String name) {
        if (!name.contains(":")) {
            name = "minecraft:" + name;
        }
        return Block.getBlockFromName(name);
    }

    private static PathingBlockType getPathingBlockType(Block block) {
        if (BlockStateInterface.isWater(block)) {
            return PathingBlockType.WATER;
        }

        if (MovementHelper.avoidWalkingInto(block)) {
            return PathingBlockType.AVOID;
        }
        // We used to do an AABB check here
        // however, this failed in the nether when you were near a nether fortress
        // because fences check their adjacent blocks in the world for their fence connection status to determine AABB shape
        // this caused a nullpointerexception when we saved chunks on unload, because they were unable to check their neighbors
        if (block instanceof BlockAir || block instanceof BlockTallGrass || block instanceof BlockDoublePlant) {
            return PathingBlockType.AIR;
        }

        return PathingBlockType.SOLID;
    }

    static IBlockState pathingTypeToBlock(PathingBlockType type) {
        if (type != null) {
            switch (type) {
                case AIR:
                    return Blocks.AIR.getDefaultState();
                case WATER:
                    return Blocks.WATER.getDefaultState();
                case AVOID:
                    return Blocks.LAVA.getDefaultState();
                case SOLID:
                    return Blocks.OBSIDIAN.getDefaultState();
            }
        }
        return null;
    }
}
