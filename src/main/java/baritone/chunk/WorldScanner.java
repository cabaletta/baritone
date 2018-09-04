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

import baritone.utils.Helper;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public enum WorldScanner implements Helper {
    INSTANCE;

    public List<BlockPos> scanLoadedChunks(List<String> blockTypes, int max) {
        List<Block> asBlocks = blockTypes.stream().map(ChunkPacker::stringToBlock).collect(Collectors.toList());
        if (asBlocks.contains(null)) {
            throw new IllegalStateException("Invalid block name should have been caught earlier: " + blockTypes.toString());
        }
        LinkedList<BlockPos> res = new LinkedList<>();
        if (asBlocks.isEmpty()) {
            return res;
        }
        ChunkProviderClient chunkProvider = world().getChunkProvider();

        int playerChunkX = playerFeet().getX() >> 4;
        int playerChunkZ = playerFeet().getZ() >> 4;

        int searchRadius = 2;
        while (true) {
            boolean allUnloaded = true;
            for (int xoff = -searchRadius; xoff <= searchRadius; xoff++) {
                for (int zoff = -searchRadius; zoff <= searchRadius; zoff++) {
                    int distance = xoff * xoff + zoff * zoff;
                    if (distance != searchRadius) {
                        continue;
                    }
                    int chunkX = xoff + playerChunkX;
                    int chunkZ = zoff + playerChunkZ;
                    Chunk chunk = chunkProvider.getLoadedChunk(chunkX, chunkZ);
                    if (chunk == null) {
                        continue;
                    }
                    allUnloaded = false;
                    ExtendedBlockStorage[] chunkInternalStorageArray = chunk.getBlockStorageArray();
                    chunkX = chunkX << 4;
                    chunkZ = chunkZ << 4;
                    for (int y0 = 0; y0 < 16; y0++) {
                        ExtendedBlockStorage extendedblockstorage = chunkInternalStorageArray[y0];
                        if (extendedblockstorage == null) {
                            continue;
                        }
                        int yReal = y0 << 4;
                        BlockStateContainer bsc = extendedblockstorage.getData();
                        // the mapping of BlockStateContainer.getIndex from xyz to index is y << 8 | z << 4 | x;
                        // for better cache locality, iterate in that order
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                for (int x = 0; x < 16; x++) {
                                    IBlockState state = bsc.get(x, y, z);
                                    if (asBlocks.contains(state.getBlock())) {
                                        res.add(new BlockPos(chunkX | x, yReal | y, chunkZ | z));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (allUnloaded) {
                return res;
            }
            if (res.size() >= max) {
                return res;
            }
            searchRadius++;
        }
    }
}
