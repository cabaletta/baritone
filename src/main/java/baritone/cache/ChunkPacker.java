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
import net.minecraft.block.*;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.palette.PalettedContainer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

import java.util.*;

import static baritone.utils.BlockStateInterface.getFromChunk;

/**
 * @author Brady
 * @since 8/3/2018
 */
public final class ChunkPacker {

    private ChunkPacker() {}

    public static CachedChunk pack(Chunk chunk) {
        //long start = System.nanoTime() / 1000000L;

        Map<String, List<BlockPos>> specialBlocks = new HashMap<>();
        BitSet bitSet = new BitSet(CachedChunk.SIZE);
        try {
            ChunkSection[] chunkInternalStorageArray = chunk.getSections();
            for (int y0 = 0; y0 < 16; y0++) {
                ChunkSection extendedblockstorage = chunkInternalStorageArray[y0];
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
                PalettedContainer<BlockState> bsc = extendedblockstorage.getData();
                int yReal = y0 << 4;
                // the mapping of BlockStateContainer.getIndex from xyz to index is y << 8 | z << 4 | x;
                // for better cache locality, iterate in that order
                for (int y1 = 0; y1 < 16; y1++) {
                    int y = y1 | yReal;
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            int index = CachedChunk.getPositionIndex(x, y, z);
                            BlockState state = bsc.get(x, y1, z);
                            boolean[] bits = getPathingBlockType(state, chunk, x, y, z).getBits();
                            bitSet.set(index, bits[0]);
                            bitSet.set(index + 1, bits[1]);
                            Block block = state.getBlock();
                            if (CachedChunk.BLOCKS_TO_KEEP_TRACK_OF.contains(block)) {
                                String name = BlockUtils.blockToString(block);
                                specialBlocks.computeIfAbsent(name, b -> new ArrayList<>()).add(new BlockPos(x, y, z));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //long end = System.nanoTime() / 1000000L;
        //System.out.println("Chunk packing took " + (end - start) + "ms for " + chunk.x + "," + chunk.z);
        BlockState[] blocks = new BlockState[256];

        // @formatter:off
        for (int z = 0; z < 16; z++) {
            https://www.ibm.com/developerworks/library/j-perry-writing-good-java-code/index.html
            for (int x = 0; x < 16; x++) {
                for (int y = 255; y >= 0; y--) {
                    int index = CachedChunk.getPositionIndex(x, y, z);
                    if (bitSet.get(index) || bitSet.get(index + 1)) {
                        blocks[z << 4 | x] = getFromChunk(chunk, x, y, z);
                        continue https;
                    }
                }
                blocks[z << 4 | x] = Blocks.AIR.getDefaultState();
            }
        }
        // @formatter:on
        return new CachedChunk(chunk.getPos().x, chunk.getPos().z, bitSet, blocks, specialBlocks, System.currentTimeMillis());
    }

    private static PathingBlockType getPathingBlockType(BlockState state, Chunk chunk, int x, int y, int z) {
        Block block = state.getBlock();
        if (MovementHelper.isWater(state)) {
            // only water source blocks are plausibly usable, flowing water should be avoid
            // FLOWING_WATER is a waterfall, it doesn't really matter and caching it as AVOID just makes it look wrong
            if (MovementHelper.possiblyFlowing(state)) {
                return PathingBlockType.AVOID;
            }
            if (
                    (x != 15 && MovementHelper.possiblyFlowing(getFromChunk(chunk, x + 1, y, z)))
                            || (x != 0 && MovementHelper.possiblyFlowing(getFromChunk(chunk, x - 1, y, z)))
                            || (z != 15 && MovementHelper.possiblyFlowing(getFromChunk(chunk, x, y, z + 1)))
                            || (z != 0 && MovementHelper.possiblyFlowing(getFromChunk(chunk, x, y, z - 1)))
            ) {
                return PathingBlockType.AVOID;
            }
            if (x == 0 || x == 15 || z == 0 || z == 15) {
                Vector3d flow = state.getFluidState().getFlow(chunk.getWorld(), new BlockPos(x + (chunk.getPos().x << 4), y, z + (chunk.getPos().z << 4)));
                if (flow.x != 0.0 || flow.z != 0.0) {
                    return PathingBlockType.WATER;
                }
                return PathingBlockType.AVOID;
            }
            return PathingBlockType.WATER;
        }

        if (MovementHelper.avoidWalkingInto(state) || MovementHelper.isBottomSlab(state)) {
            return PathingBlockType.AVOID;
        }
        // We used to do an AABB check here
        // however, this failed in the nether when you were near a nether fortress
        // because fences check their adjacent blocks in the world for their fence connection status to determine AABB shape
        // this caused a nullpointerexception when we saved chunks on unload, because they were unable to check their neighbors
        if (block instanceof AirBlock || block instanceof TallGrassBlock || block instanceof DoublePlantBlock || block instanceof FlowerBlock) {
            return PathingBlockType.AIR;
        }

        return PathingBlockType.SOLID;
    }

    public static BlockState pathingTypeToBlock(PathingBlockType type, RegistryKey<World> dimension) {
        switch (type) {
            case AIR:
                return Blocks.AIR.getDefaultState();
            case WATER:
                return Blocks.WATER.getDefaultState();
            case AVOID:
                return Blocks.LAVA.getDefaultState();
            case SOLID:
                // Dimension solid types
                if (dimension == World.OVERWORLD) {
                    return Blocks.STONE.getDefaultState();
                }
                if (dimension == World.THE_NETHER) {
                    return Blocks.NETHERRACK.getDefaultState();
                }
                if (dimension == World.THE_END) {
                    return Blocks.END_STONE.getDefaultState();
                }
            default:
                return null;
        }
    }
}
