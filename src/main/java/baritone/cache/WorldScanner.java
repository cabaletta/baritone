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

import baritone.api.cache.IWorldScanner;
import baritone.api.utils.IPlayerContext;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public enum WorldScanner implements IWorldScanner {

    INSTANCE;

    @Override
    public List<BlockPos> scanChunkRadius(IPlayerContext ctx, List<Block> blocks, int max, int yLevelThreshold, int maxSearchRadius) {
        if (blocks.contains(null)) {
            throw new IllegalStateException("Invalid block name should have been caught earlier: " + blocks.toString());
        }
        ArrayList<BlockPos> res = new ArrayList<>();
        if (blocks.isEmpty()) {
            return res;
        }
        ChunkProviderClient chunkProvider = (ChunkProviderClient) ctx.world().getChunkProvider();

        int maxSearchRadiusSq = maxSearchRadius * maxSearchRadius;
        int playerChunkX = ctx.playerFeet().getX() >> 4;
        int playerChunkZ = ctx.playerFeet().getZ() >> 4;
        int playerY = ctx.playerFeet().getY();

        int searchRadiusSq = 0;
        boolean foundWithinY = false;
        while (true) {
            boolean allUnloaded = true;
            boolean foundChunks = false;
            for (int xoff = -searchRadiusSq; xoff <= searchRadiusSq; xoff++) {
                for (int zoff = -searchRadiusSq; zoff <= searchRadiusSq; zoff++) {
                    int distance = xoff * xoff + zoff * zoff;
                    if (distance != searchRadiusSq) {
                        continue;
                    }
                    foundChunks = true;
                    int chunkX = xoff + playerChunkX;
                    int chunkZ = zoff + playerChunkZ;
                    Chunk chunk = chunkProvider.getChunk(chunkX, chunkZ, false, false);
                    if (chunk == null) {
                        continue;
                    }
                    allUnloaded = false;
                    scanChunkInto(chunkX << 4, chunkZ << 4, chunk, blocks, res, max, yLevelThreshold, playerY);
                }
            }
            if ((allUnloaded && foundChunks)
                    || (res.size() >= max
                    && (searchRadiusSq > maxSearchRadiusSq || (searchRadiusSq > 1 && foundWithinY)))
            ) {
                return res;
            }
            searchRadiusSq++;
        }
    }

    @Override
    public List<BlockPos> scanChunk(IPlayerContext ctx, List<Block> blocks, ChunkPos pos, int max, int yLevelThreshold) {
        if (blocks.isEmpty()) {
            return Collections.emptyList();
        }

        ChunkProviderClient chunkProvider = (ChunkProviderClient) ctx.world().getChunkProvider();
        Chunk chunk = chunkProvider.getChunk(pos.x, pos.z, false, false);
        int playerY = ctx.playerFeet().getY();

        if (chunk == null || chunk.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<BlockPos> res = new ArrayList<>();
        scanChunkInto(pos.x << 4, pos.z << 4, chunk, blocks, res, max, yLevelThreshold, playerY);
        return res;
    }

    public void scanChunkInto(int chunkX, int chunkZ, Chunk chunk, List<Block> search, Collection<BlockPos> result, int max, int yLevelThreshold, int playerY) {
        ChunkSection[] chunkInternalStorageArray = chunk.getSections();
        for (int y0 = 0; y0 < 16; y0++) {
            ChunkSection extendedblockstorage = chunkInternalStorageArray[y0];
            if (extendedblockstorage == null) {
                continue;
            }
            int yReal = y0 << 4;
            BlockStateContainer<IBlockState> bsc = extendedblockstorage.getData();
            // the mapping of BlockStateContainer.getIndex from xyz to index is y << 8 | z << 4 | x;
            // for better cache locality, iterate in that order
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        IBlockState state = bsc.get(x, y, z);
                        if (search.contains(state.getBlock())) {
                            int yy = yReal | y;
                            result.add(new BlockPos(chunkX | x, yy, chunkZ | z));
                            if (result.size() >= max && Math.abs(yy - playerY) < yLevelThreshold) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
