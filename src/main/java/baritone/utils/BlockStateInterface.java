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
import baritone.utils.accessor.IClientChunkProvider;
import baritone.utils.pathing.BetterWorldBorder;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.Arrays;

/**
 * Wraps get for chuck caching capability
 *
 * @author leijurv
 */
public class BlockStateInterface {

    private final ClientChunkCache provider;
    private final WorldData worldData;
    protected final Level world;
    public final BlockPos.MutableBlockPos isPassableBlockPos;
    public final BlockGetter access;
    public final BetterWorldBorder worldBorder;

    private LevelChunk prev = null;
    private CachedRegion prevCached = null;

    private final boolean useTheRealWorld;

    // world.dimensionType() goes through a Holder and we were calling it twice per block. per block!
    public final int minY;
    public final int maxY;
    private final int height;

    // position -> state cache, only while a search has claimed it (null keys = no cache)
    // the per tick ones on the main thread look up like four blocks and get garbage collected, not worth it
    // 64k entries hits ~80% of the time, 16k only managed 73%, so 64k it is
    private static final int CACHE_BITS = 16;
    private long[] cacheKeys;
    private BlockState[] cacheVals;
    // whoever claimed it. everyone else goes around, same deal as CalculationContext.claimSearchCaches
    private Thread cacheOwner;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    public BlockStateInterface(IPlayerContext ctx) {
        this(ctx, false);
    }

    public BlockStateInterface(IPlayerContext ctx, boolean copyLoadedChunks) {
        this.world = ctx.world();
        this.worldBorder = new BetterWorldBorder(world.getWorldBorder());
        this.worldData = (WorldData) ctx.worldData();
        if (copyLoadedChunks) {
            this.provider = ((IClientChunkProvider) world.getChunkSource()).createThreadSafeCopy();
        } else {
            this.provider = (ClientChunkCache) world.getChunkSource();
        }
        this.useTheRealWorld = !Baritone.settings().pathThroughCachedOnly.value;
        if (!ctx.minecraft().isSameThread()) {
            throw new IllegalStateException("BlockStateInterface must be constructed on the main thread");
        }
        this.minY = world.dimensionType().minY();
        this.height = world.dimensionType().height();
        this.maxY = minY + height - 1;
        this.isPassableBlockPos = new BlockPos.MutableBlockPos();
        this.access = new BlockStateInterfaceAccessWrapper(this);
    }

    // GameEventHandler builds a copying one of these every tick and this used to be allocated in the constructor
    // 768kb a tick so the main thread could look up four blocks. now only a search asks for it
    public synchronized void claimCache() {
        if (cacheOwner != null) {
            return;
        }
        long[] keys = new long[1 << CACHE_BITS];
        // -1 would be a block at shifted y=4095. good luck
        Arrays.fill(keys, -1L);
        cacheKeys = keys;
        cacheVals = new BlockState[1 << CACHE_BITS];
        cacheOwner = Thread.currentThread();
    }

    public synchronized void releaseCache() {
        if (cacheOwner != Thread.currentThread()) {
            return;
        }
        cacheOwner = null;
        cacheKeys = null;
        cacheVals = null;
    }

    // for subclasses that get their blocks from somewhere that isn't a client world (benchmarks, tests)
    // there's no chunk provider so you have to override getUncached, isLoaded and worldContainsLoadedChunk or it'll npe
    protected BlockStateInterface(BetterWorldBorder worldBorder, int minY, int height) {
        this.world = null;
        this.worldBorder = worldBorder;
        this.worldData = null;
        this.provider = null;
        this.useTheRealWorld = true;
        this.minY = minY;
        this.height = height;
        this.maxY = minY + height - 1;
        this.isPassableBlockPos = new BlockPos.MutableBlockPos();
        this.access = new BlockStateInterfaceAccessWrapper(this);
    }

    public boolean worldContainsLoadedChunk(int blockX, int blockZ) {
        return provider.hasChunk(blockX >> 4, blockZ >> 4);
    }

    public static Block getBlock(IPlayerContext ctx, BlockPos pos) { // won't be called from the pathing thread because the pathing thread doesn't make a single blockpos pog
        return get(ctx, pos).getBlock();
    }

    public static BlockState get(IPlayerContext ctx, BlockPos pos) {
        return new BlockStateInterface(ctx).get0(pos.getX(), pos.getY(), pos.getZ()); // immense iq
        // can't just do world().get because that doesn't work for out of bounds
        // and toBreak and stuff fails when the movement is instantiated out of load range but it's not able to BlockStateInterface.get what it's going to walk on
    }

    public BlockState get0(BlockPos pos) {
        return get0(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockState get0(int x, int y, int z) { // Mickey resigned
        y -= minY;
        // Invalid vertical position
        if (y < 0 || y >= height) {
            return AIR;
        }
        long[] keys = cacheKeys;
        if (keys == null || cacheOwner != Thread.currentThread()) {
            return getUncached(x, y, z);
        }
        // the 22 movements out of one node read the same 50 blocks about 110 times between them
        // and then the next node reads most of them again. so, cache
        long key = ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | y;
        int slot = (int) ((key * 0x9E3779B97F4A7C15L) >>> (64 - CACHE_BITS));
        if (keys[slot] == key) {
            return cacheVals[slot];
        }
        BlockState state = getUncached(x, y, z);
        keys[slot] = key;
        cacheVals[slot] = state;
        return state;
    }

    // y is already shifted so 0 is bedrock, and already known to be in range
    protected BlockState getUncached(int x, int y, int z) {
        if (useTheRealWorld) {
            LevelChunk cached = prev;
            // there's great cache locality in block state lookups
            // generally it's within each movement
            // if it's the same chunk as last time
            // we can just skip the mc.world.getChunk lookup
            // which is a Long2ObjectOpenHashMap.get
            // see issue #113
            if (cached != null && cached.getPos().x == x >> 4 && cached.getPos().z == z >> 4) {
                return getFromChunk(cached, x, y, z);
            }
            LevelChunk chunk = provider.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, false);
            if (chunk != null && !chunk.isEmpty()) {
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
        BlockState type = cached.getBlock(x & 511, y + minY, z & 511);
        if (type == null) {
            return AIR;
        }
        return type;
    }

    public boolean isLoaded(int x, int z) {
        LevelChunk prevChunk = prev;
        if (prevChunk != null && prevChunk.getPos().x == x >> 4 && prevChunk.getPos().z == z >> 4) {
            return true;
        }
        prevChunk = provider.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, false);
        if (prevChunk != null && !prevChunk.isEmpty()) {
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
    public static BlockState getFromChunk(LevelChunk chunk, int x, int y, int z) {
        return getFromChunk(chunk.getSections(), x, y, z);
    }

    public static BlockState getFromChunk(LevelChunkSection[] sections, int x, int y, int z) {
        LevelChunkSection section = sections[y >> 4];
        if (section.hasOnlyAir()) {
            return AIR;
        }
        return section.getBlockState(x & 15, y & 15, z & 15);
    }
}
