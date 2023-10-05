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
import baritone.utils.accessor.IPalettedContainer;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.IdMapper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.GlobalPalette;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum FasterWorldScanner implements IWorldScanner {
    INSTANCE;
    @Override
    public List<BlockPos> scanChunkRadius(IPlayerContext ctx, BlockOptionalMetaLookup filter, int max, int yLevelThreshold, int maxSearchRadius) {
        assert ctx.world() != null;
        if (maxSearchRadius < 0) {
            throw new IllegalArgumentException("chunkRange must be >= 0");
        }
        return scanChunksInternal(ctx, filter, getChunkRange(ctx.playerFeet().x >> 4, ctx.playerFeet().z >> 4, maxSearchRadius), max);
    }

    @Override
    public List<BlockPos> scanChunk(IPlayerContext ctx, BlockOptionalMetaLookup filter, ChunkPos pos, int max, int yLevelThreshold) {
        Stream<BlockPos> stream = scanChunkInternal(ctx, filter, pos);
        if (max >= 0) {
            stream = stream.limit(max);
        }
        return stream.collect(Collectors.toList());
    }

    @Override
    public int repack(IPlayerContext ctx) {
        return this.repack(ctx, 40);
    }

    @Override
    public int repack(IPlayerContext ctx, int range) {
        ChunkSource chunkProvider = ctx.world().getChunkSource();
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
                LevelChunk chunk = chunkProvider.getChunk(x, z, false);

                if (chunk != null && !chunk.isEmpty()) {
                    queued++;
                    cachedWorld.queueForPacking(chunk);
                }
            }
        }

        return queued;
    }

    // ordered in a way that the closest blocks are generally first
    public static List<ChunkPos> getChunkRange(int centerX, int centerZ, int chunkRadius) {
        List<ChunkPos> chunks = new ArrayList<>();
        // spiral out
        chunks.add(new ChunkPos(centerX, centerZ));
        for (int i = 1; i < chunkRadius; i++) {
            for (int j = 0; j <= i; j++) {
                chunks.add(new ChunkPos(centerX - j, centerZ - i));
                if (j != 0) {
                    chunks.add(new ChunkPos(centerX + j, centerZ - i));
                    chunks.add(new ChunkPos(centerX - j, centerZ + i));
                }
                chunks.add(new ChunkPos(centerX + j, centerZ + i));
                if (j != i) {
                    chunks.add(new ChunkPos(centerX - i, centerZ - j));
                    chunks.add(new ChunkPos(centerX + i, centerZ - j));
                    if (j != 0) {
                        chunks.add(new ChunkPos(centerX - i, centerZ + j));
                        chunks.add(new ChunkPos(centerX + i, centerZ + j));
                    }
                }
            }
        }
        return chunks;
    }

    private List<BlockPos> scanChunksInternal(IPlayerContext ctx, BlockOptionalMetaLookup lookup, List<ChunkPos> chunkPositions, int maxBlocks) {
        assert ctx.world() != null;
        try {
            // p -> scanChunkInternal(ctx, lookup, p)
            Stream<BlockPos> posStream = chunkPositions.parallelStream().flatMap(p -> scanChunkInternal(ctx, lookup, p));
            if (maxBlocks >= 0) {
                // WARNING: this can be expensive if maxBlocks is large...
                // see limit's javadoc
                posStream = posStream.limit(maxBlocks);
            }
            return posStream.collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private Stream<BlockPos> scanChunkInternal(IPlayerContext ctx, BlockOptionalMetaLookup lookup, ChunkPos pos) {
        ChunkSource chunkProvider = ctx.world().getChunkSource();
        // if chunk is not loaded, return empty stream
        if (!chunkProvider.hasChunk(pos.x, pos.z)) {
            return Stream.empty();
        }

        long chunkX = (long) pos.x << 4;
        long chunkZ = (long) pos.z << 4;

        int playerSectionY = (ctx.playerFeet().y - ctx.world().getMinBuildHeight()) >> 4;

        return collectChunkSections(lookup, chunkProvider.getChunk(pos.x, pos.z, false), chunkX, chunkZ, playerSectionY).stream();
    }



    private List<BlockPos> collectChunkSections(BlockOptionalMetaLookup lookup, LevelChunk chunk, long chunkX, long chunkZ, int playerSection) {
        // iterate over sections relative to player
        List<BlockPos> blocks = new ArrayList<>();
        LevelChunkSection[] sections = chunk.getSections();
        int l = sections.length;
        int i = playerSection - 1;
        int j = playerSection;
        for (; i >= 0 || j < l; ++j, --i) {
            if (j < l) {
                visitSection(lookup, sections[j], blocks, chunkX, chunkZ);
            }
            if (i >= 0) {
                visitSection(lookup, sections[i], blocks, chunkX, chunkZ);
            }
        }
        return blocks;
    }

    private void visitSection(BlockOptionalMetaLookup lookup, LevelChunkSection section, List<BlockPos> blocks, long chunkX, long chunkZ) {
        if (section == null || section.hasOnlyAir()) {
            return;
        }

        PalettedContainer<BlockState> sectionContainer = section.getStates();
        //this won't work if the PaletteStorage is of the type EmptyPaletteStorage
        if (((IPalettedContainer<BlockState>) sectionContainer).getStorage() == null) {
            return;
        }

        boolean[] isInFilter = getIncludedFilterIndices(lookup, ((IPalettedContainer<BlockState>) sectionContainer).getPalette());
        if (isInFilter.length == 0) {
            return;
        }

        BitStorage array = ((IPalettedContainer<BlockState>) section.getStates()).getStorage();
        long[] longArray = array.getRaw();
        int arraySize = array.getSize();
        int bitsPerEntry = array.getBits();
        long maxEntryValue = (1L << bitsPerEntry) - 1L;


        int yOffset = section.bottomBlockY();

        for (int i = 0, idx = 0; i < longArray.length && idx < arraySize; ++i) {
            long l = longArray[i];
            for (int offset = 0; offset <= (64 - bitsPerEntry) && idx < arraySize; offset += bitsPerEntry, ++idx) {
                int value = (int) ((l >> offset) & maxEntryValue);
                if (isInFilter[value]) {
                    //noinspection DuplicateExpressions
                    blocks.add(new BlockPos(
                        chunkX + ((idx & 255) & 15),
                        yOffset + (idx >> 8),
                        chunkZ + ((idx & 255) >> 4)
                    ));
                }
            }
        }
    }

    private boolean[] getIncludedFilterIndices(BlockOptionalMetaLookup lookup, Palette<BlockState> palette) {
        boolean commonBlockFound = false;
        IdMapper<BlockState> paletteMap = getPalette(palette);
        int size = paletteMap.size();

        boolean[] isInFilter = new boolean[size];

        for (int i = 0; i < size; i++) {
            BlockState state = paletteMap.byId(i);
            if (lookup.has(state)) {
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

    /**
     * cheats to get the actual map of id -> blockstate from the various palette implementations
     */
    private static IdMapper<BlockState> getPalette(Palette<BlockState> palette) {
        if (palette instanceof GlobalPalette) {
            return Block.BLOCK_STATE_REGISTRY;
        } else {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            palette.write(buf);
            int size = buf.readVarInt();
            IdMapper<BlockState> states = new IdMapper<>();
            for (int i = 0; i < size; i++) {
                BlockState state = Block.BLOCK_STATE_REGISTRY.byId(buf.readVarInt());
                assert state != null;
                states.addMapping(state, i);
            }
            return states;
        }
    }
}
