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
import baritone.utils.Helper;
import baritone.utils.pathing.PathingBlockType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.*;

/**
 * @author Brady
 * @since 8/3/2018 1:09 AM
 */
public final class ChunkPacker implements Helper {

    private ChunkPacker() {}

    private static BitSet originalPacker(Chunk chunk) {
        BitSet bitSet = new BitSet(CachedChunk.SIZE);
        for (int y = 0; y < 256; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int index = CachedChunk.getPositionIndex(x, y, z);
                    IBlockState state = chunk.getBlockState(x, y, z);
                    boolean[] bits = getPathingBlockType(state).getBits();
                    bitSet.set(index, bits[0]);
                    bitSet.set(index + 1, bits[1]);
                }
            }
        }
        return bitSet;
    }

    public static CachedChunk pack(Chunk chunk) {
        long start = System.nanoTime() / 1000000L;

        Map<String, List<BlockPos>> specialBlocks = new HashMap<>();
        BitSet bitSet = new BitSet(CachedChunk.SIZE);
        try {
            ExtendedBlockStorage[] chunkInternalStorageArray = chunk.getBlockStorageArray();
            for (int y0 = 0; y0 < 16; y0++) {
                ExtendedBlockStorage extendedblockstorage = chunkInternalStorageArray[y0];
                if (extendedblockstorage == null) {
                    // any 16x16x16 area that's all air will have null storage
                    // for example, in an ocean biome, with air from y=64 to y=256
                    // the first 4 extended blocks storages will be full
                    // and the remaining 12 will be null

                    // since the index into the bitset is calculated from the x y and z
                    // and doesn't function as an append, we can entirely skip the scanning
                    // since a bitset is initialized to all zero, and air is saved as zeros
                    continue;
                }
                BlockStateContainer bsc = extendedblockstorage.getData();
                int yReal = y0 << 4;
                for (int x = 0; x < 16; x++) {
                    for (int y1 = 0; y1 < 16; y1++) {
                        for (int z = 0; z < 16; z++) {
                            int y = y1 | yReal;
                            int index = CachedChunk.getPositionIndex(x, y, z);
                            IBlockState state = bsc.get(x, y1, z);
                            boolean[] bits = getPathingBlockType(state).getBits();
                            bitSet.set(index, bits[0]);
                            bitSet.set(index + 1, bits[1]);
                            Block block = state.getBlock();
                            if (CachedChunk.BLOCKS_TO_KEEP_TRACK_OF.contains(block)) {
                                String name = blockToString(block);
                                specialBlocks.computeIfAbsent(name, b -> new ArrayList<>()).add(new BlockPos(x, y, z));
                            }
                        }
                    }
                }
            }
            /*if (!bitSet.equals(originalPacker(chunk))) {
                throw new IllegalStateException();
            }*/
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println("Packed special blocks: " + specialBlocks);
        long end = System.nanoTime() / 1000000L;
        //System.out.println("Chunk packing took " + (end - start) + "ms for " + chunk.x + "," + chunk.z);
        String[] blockNames = new String[256];
        for (int z = 0; z < 16; z++) {
            outerLoop:
            for (int x = 0; x < 16; x++) {
                for (int y = 255; y >= 0; y--) {
                    int index = CachedChunk.getPositionIndex(x, y, z);
                    if (bitSet.get(index) || bitSet.get(index + 1)) {
                        String name = blockToString(chunk.getBlockState(x, y, z).getBlock());
                        blockNames[z << 4 | x] = name;
                        continue outerLoop;
                    }
                }
                blockNames[z << 4 | x] = "air";
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

    private static PathingBlockType getPathingBlockType(IBlockState state) {
        Block block = state.getBlock();
        if (block.equals(Blocks.WATER)) {
            // only water source blocks are plausibly usable, flowing water should be avoid
            return PathingBlockType.WATER;
        }

        if (MovementHelper.avoidWalkingInto(block) || block.equals(Blocks.FLOWING_WATER) || MovementHelper.isBottomSlab(state)) {
            return PathingBlockType.AVOID;
        }
        // We used to do an AABB check here
        // however, this failed in the nether when you were near a nether fortress
        // because fences check their adjacent blocks in the world for their fence connection status to determine AABB shape
        // this caused a nullpointerexception when we saved chunks on unload, because they were unable to check their neighbors
        if (block == Blocks.AIR || block instanceof BlockTallGrass || block instanceof BlockDoublePlant || block instanceof BlockFlower) {
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
                    // Dimension solid types
                    switch (mc.player.dimension) {
                        case -1:
                            return Blocks.NETHERRACK.getDefaultState();
                        case 0:
                            return Blocks.STONE.getDefaultState();
                        case 1:
                            return Blocks.END_STONE.getDefaultState();
                    }

                    // The fallback solid type
                    return Blocks.STONE.getDefaultState();
            }
        }
        return null;
    }
}
