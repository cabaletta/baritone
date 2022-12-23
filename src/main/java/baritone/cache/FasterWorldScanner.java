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

import baritone.api.cache.ICachedWorld;
import baritone.api.cache.IWorldScanner;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.api.utils.IPlayerContext;
import baritone.utils.accessor.IBitArray;
import baritone.utils.accessor.IBlockStateContainer;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.BitArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum FasterWorldScanner implements IWorldScanner {
    INSTANCE;
    @Override
    public List<BlockPos> scanChunkRadius(IPlayerContext ctx, BlockOptionalMetaLookup filter, int max, int yLevelThreshold, int maxSearchRadius) {
        return new WorldScannerContext(filter, ctx).scanAroundPlayerRange(maxSearchRadius);
    }

    @Override
    public List<BlockPos> scanChunk(IPlayerContext ctx, BlockOptionalMetaLookup filter, ChunkPos pos, int max, int yLevelThreshold) {
        return new WorldScannerContext(filter, ctx).scanAroundPlayerUntilCount(max);
    }

    @Override
    public int repack(IPlayerContext ctx) {
        return this.repack(ctx, 40);
    }

    @Override
    public int repack(IPlayerContext ctx, int range) {
        IChunkProvider chunkProvider = ctx.world().getChunkProvider();
        ICachedWorld cachedWorld = ctx.worldData().getCachedWorld();

        BetterBlockPos playerPos = ctx.playerFeet();

        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;

        int minX = playerChunkX - range;
        int minZ = playerChunkZ - range;
        int maxX = playerChunkX + range;
        int maxZ = playerChunkZ + range;

        int queued = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Chunk chunk = chunkProvider.getLoadedChunk(x, z);

                if (chunk != null && !chunk.isEmpty()) {
                    queued++;
                    cachedWorld.queueForPacking(chunk);
                }
            }
        }

        return queued;
    }

    // for porting, see {@link https://github.com/JsMacros/JsMacros/blob/backport-1.12.2/common/src/main/java/xyz/wagyourtail/jsmacros/client/api/classes/worldscanner/WorldScanner.java}
    public static class WorldScannerContext {
        private final BlockOptionalMetaLookup filter;
        private final IPlayerContext ctx;
        private final Map<IBlockState, Boolean> cachedFilter = new ConcurrentHashMap<>();

        public WorldScannerContext(BlockOptionalMetaLookup filter, IPlayerContext ctx) {
            this.filter = filter;
            this.ctx = ctx;
        }

        public List<ChunkPos> getChunkRange(int centerX, int centerZ, int chunkrange) {
            List<ChunkPos> chunks = new ArrayList<>();
            for (int x = centerX - chunkrange; x <= centerX + chunkrange; x++) {
                for (int z = centerZ - chunkrange; z <= centerZ + chunkrange; z++) {
                    chunks.add(new ChunkPos(x, z));
                }
            }
            return chunks;
        }

        public List<BlockPos> scanAroundPlayerRange(int range) {
            return scanAroundPlayer(range, -1);
        }

        public List<BlockPos> scanAroundPlayerUntilCount(int count) {
            return scanAroundPlayer(32, count);
        }

        public List<BlockPos> scanAroundPlayer(int range, int maxCount) {
            assert ctx.player() != null;
            return scanChunkRange(ctx.playerFeet().x >> 4, ctx.playerFeet().z >> 4, range, maxCount);
        }

        public List<BlockPos> scanChunkRange(int centerX, int centerZ, int chunkRange, int maxBlocks) {
            assert ctx.world() != null;
            if (chunkRange < 0) {
                throw new IllegalArgumentException("chunkRange must be >= 0");
            }
            return scanChunksInternal(getChunkRange(centerX, centerZ, chunkRange), maxBlocks);
        }

        private List<BlockPos> scanChunksInternal(List<ChunkPos> chunkPositions, int maxBlocks) {
            assert ctx.world() != null;
            return chunkPositions.parallelStream().flatMap(this::scanChunkInternal).limit(maxBlocks <= 0 ? Long.MAX_VALUE : maxBlocks).collect(Collectors.toList());
        }
        private Stream<BlockPos> scanChunkInternal(ChunkPos pos) {
            IChunkProvider chunkProvider = ctx.world().getChunkProvider();
            // if chunk is not loaded, return empty stream
            if (!chunkProvider.isChunkGeneratedAt(pos.x, pos.z)) {
                return Stream.empty();
            }

            long chunkX = (long) pos.x << 4;
            long chunkZ = (long) pos.z << 4;

            List<BlockPos> blocks = new ArrayList<>();

            streamChunkSections(chunkProvider.getLoadedChunk(pos.x, pos.z), (section, isInFilter) -> {
                int yOffset = section.getYLocation();
                BitArray array = (BitArray) ((IBlockStateContainer) section.getData()).getStorage();
                forEach(array, isInFilter, place -> blocks.add(new BlockPos(
                        chunkX + ((place & 255) & 15),
                        yOffset + (place >> 8),
                        chunkZ + ((place & 255) >> 4)
                )));
            });
            return blocks.stream();
        }

        private void streamChunkSections(Chunk chunk, BiConsumer<ExtendedBlockStorage, boolean[]> consumer) {
            for (ExtendedBlockStorage section : chunk.getBlockStorageArray()) {
                if (section == null || section.isEmpty()) {
                    continue;
                }

                BlockStateContainer sectionContainer = section.getData();
                //this won't work if the PaletteStorage is of the type EmptyPaletteStorage
                if (((IBlockStateContainer) sectionContainer).getStorage() == null) {
                    continue;
                }

                boolean[] isInFilter = getIncludedFilterIndices(((IBlockStateContainer) sectionContainer).getPalette());
                if (isInFilter.length == 0) {
                    continue;
                }
                consumer.accept(section, isInFilter);
            }
        }

        private boolean getFilterResult(IBlockState state) {
            Boolean v;
            return (v = cachedFilter.get(state)) == null ? addCachedState(state) : v;
        }

        private boolean addCachedState(IBlockState state) {
            boolean isInFilter = false;

            if (filter != null) {
                isInFilter = filter.has(state);
            }

            cachedFilter.put(state, isInFilter);
            return isInFilter;
        }

        private boolean[] getIncludedFilterIndices(IBlockStatePalette palette) {
            boolean commonBlockFound = false;
            Int2ObjectOpenHashMap<IBlockState> paletteMap = getPalette(palette);
            int size = paletteMap.size();

            boolean[] isInFilter = new boolean[size];

            for (int i = 0; i < size; i++) {
                IBlockState state = paletteMap.get(i);
                if (getFilterResult(state)) {
                    isInFilter[i] = true;
                    commonBlockFound = true;
                } else {
                    isInFilter[i] = false;
                }
            }

            if (!commonBlockFound) {
                return new boolean[0];
            }
            return isInFilter;
        }

        private static void forEach(BitArray array, boolean[] isInFilter, IntConsumer action) {
            int counter = 0;
            long[] storage = array.getBackingLongArray();
            int arraySize = array.size();
            int elementBits = ((IBitArray) array).getBitsPerEntry();
            long maxValue = ((IBitArray) array).getMaxEntryValue();
            int storageLength = storage.length;

            if (storageLength != 0) {
                int lastStorageIdx = 0;
                long row = storage[0];
                long nextRow = storageLength > 1 ? storage[1] : 0L;

                for (int idx = 0; idx < arraySize; idx++) {
                    int n = idx * elementBits;
                    int storageIdx = n >> 6;
                    int p = (idx + 1) * elementBits - 1 >> 6;
                    int q = n ^ storageIdx << 6;
                    if (storageIdx != lastStorageIdx) {
                        row = nextRow;
                        nextRow = storageIdx + 1 < storageLength ? storage[storageIdx + 1] : 0L;
                        lastStorageIdx = storageIdx;
                    }
                    if (storageIdx == p) {
                        if (isInFilter[(int) (row >>> q & maxValue)]) {
                            action.accept(counter);
                        } else {
                            if (isInFilter[(int) ((row >>> q | nextRow << (64 - q)) & maxValue)]) {
                                action.accept(counter);
                            }
                        }
                    }
                }
            }
        }

        private static Int2ObjectOpenHashMap<IBlockState> getPalette(IBlockStatePalette palette) {
            PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
            palette.write(buf);
            int size = buf.readVarInt();
            Int2ObjectOpenHashMap<IBlockState> paletteMap = new Int2ObjectOpenHashMap<>(size);
            for (int i = 0; i < size; i++) {
                paletteMap.put(i, Block.BLOCK_STATE_IDS.getByValue(buf.readVarInt()));
            }
            return paletteMap;
        }
    }
}
