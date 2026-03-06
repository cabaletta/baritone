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

package baritone.utils;

import baritone.Baritone;
import baritone.api.utils.IPlayerContext;
import baritone.cache.CachedRegion;
import baritone.cache.WorldData;
import baritone.api.utils.BlockPos; // assume ported

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.init.Blocks;

/**
 * Wraps get for chunk caching capability
 *
 * @author leijurv
 */
public class BlockStateInterface {

    private final World provider;
    private final WorldData worldData;
    protected final World world;
    public final BetterWorldBorder worldBorder;

    private Chunk prev = null;
    private CachedRegion prevCached = null;

    private final boolean useTheRealWorld;

    private static final Block AIR = Blocks.air;

    public BlockStateInterface(IPlayerContext ctx) {
        this(ctx, false);
    }

    public BlockStateInterface(IPlayerContext ctx, boolean copyLoadedChunks) {
        this.world = ctx.world();
        this.worldBorder = new BetterWorldBorder(world.getWorldBorder());
        this.worldData = (WorldData) ctx.worldData();
        this.useTheRealWorld = !Baritone.settings().pathThroughCachedOnly.value;
        if (!ctx.minecraft().isSameThread()) {
            throw new IllegalStateException("BlockStateInterface must be constructed on the main thread");
        }
    }

    public boolean worldContainsLoadedChunk(int blockX, int blockZ) {
        return world.getChunkFromBlockCoords(blockX, blockZ) != null;
    }

    public static Block getBlock(IPlayerContext ctx, BlockPos pos) { // won't be called from the pathing thread because the pathing thread doesn't make a single blockpos pog
        return get(ctx, pos);
    }

    public static Block get(IPlayerContext ctx, BlockPos pos) {
        return new BlockStateInterface(ctx).get0(pos.getX(), pos.getY(), pos.getZ()); // immense iq
        // can't just do world().get because that doesn't work for out of bounds
        // and toBreak and stuff fails when the movement is instantiated out of load range but it's not able to BlockStateInterface.get what it's going to walk on
    }

    public Block get0(BlockPos pos) {
        return get0(pos.getX(), pos.getY(), pos.getZ());
    }

    public Block get0(int x, int y, int z) { // Mickey resigned
        // Invalid vertical position
        if (y < 0 || y >= 256) {
            return AIR;
        }

        if (useTheRealWorld) {
            Chunk cached = prev;
            // there's great cache locality in block state lookups
            // generally it's within each movement
            // if it's the same chunk as last time
            // we can just skip the mc.world.getChunk lookup
            // which is a Long2ObjectOpenHashMap.get
            // see issue #113
            if (cached != null && cached.xPosition == x >> 4 && cached.zPosition == z >> 4) {
                return getFromChunk(cached, x, y, z);
            }
            Chunk chunk = world.getChunkFromBlockCoords(x, z);
            if (chunk != null) {
                prev = chunk;
                return getFromChunk(chunk, x, y, z);
            }
        }
        // same idea here, skip the Long2ObjectOpenHashMap.get if at all possible
        // except here, it's 512x512 tiles instead of 16x16, so even better repetition
        CachedRegion cached = prevCached;
        if (cached == null || cached.getX() != x >> 9 || cached.getZ() != z >> 9) {
            if (worldData == null) {
                return AIR;
            }
            CachedRegion region = worldData.cache.getRegion(x >> 9, z >> 9);
            if (region == null) {
                return AIR;
            }
            prevCached = region;
            cached = region;
        }
        Block block = cached.getBlock(x & 511, y, z & 511);
        if (block == null) {
            return AIR;
        }
        return block;
    }

    public boolean isLoaded(int x, int z) {
        Chunk prevChunk = prev;
        if (prevChunk != null && prevChunk.xPosition == x >> 4 && prevChunk.zPosition == z >> 4) {
            return true;
        }
        prevChunk = world.getChunkFromBlockCoords(x, z);
        if (prevChunk != null) {
            prev = prevChunk;
            return true;
        }
        CachedRegion prevRegion = prevCached;
        if (prevRegion != null && prevRegion.getX() == x >> 9 && prevRegion.getZ() == z >> 9) {
            return prevRegion.isCached(x & 511, z & 511);
        }
        if (worldData == null) {
            return false;
        }
        prevRegion = worldData.cache.getRegion(x >> 9, z >> 9);
        if (prevRegion == null) {
            return false;
        }
        prevCached = prevRegion;
        return prevRegion.isCached(x & 511, z & 511);
    }

    // get the block at x,y,z from this chunk WITHOUT creating a single blockpos object
    public static Block getFromChunk(Chunk chunk, int x, int y, int z) {
        int sectionY = y >> 4;
        ExtendedBlockStorage section = chunk.getBlockStorageArray()[sectionY];
        if (section == null) return AIR;
        int id = section.getExtBlockID(x & 15, y & 15, z & 15);
        if (id == 0) return AIR;
        return Block.getBlockById(id);
    }
}