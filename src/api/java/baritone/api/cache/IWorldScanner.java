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

package baritone.api.cache;

import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.api.utils.IPlayerContext;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.List;

/**
 * @author Brady
 * @since 10/6/2018
 */
public interface IWorldScanner {

    /**
     * Scans the world, up to the specified max chunk radius, for the specified blocks.
     *
     * @param ctx             The {@link IPlayerContext} containing player and world info that the scan is based upon
     * @param filter          The blocks to scan for
     * @param max             The maximum number of blocks to scan before cutoff
     * @param yLevelThreshold If a block is found within this Y level, the current result will be returned, if the value
     *                        is negative, then this condition doesn't apply.
     * @param maxSearchRadius The maximum chunk search radius
     * @return The matching block positions
     */
    List<BlockPos> scanChunkRadius(IPlayerContext ctx, BlockOptionalMetaLookup filter, int max, int yLevelThreshold, int maxSearchRadius);

    default List<BlockPos> scanChunkRadius(IPlayerContext ctx, List<Block> filter, int max, int yLevelThreshold, int maxSearchRadius) {
        return scanChunkRadius(ctx, new BlockOptionalMetaLookup(filter.toArray(new Block[0])), max, yLevelThreshold, maxSearchRadius);
    }

    /**
     * Scans a single chunk for the specified blocks.
     *
     * @param ctx             The {@link IPlayerContext} containing player and world info that the scan is based upon
     * @param filter          The blocks to scan for
     * @param pos             The position of the target chunk
     * @param max             The maximum number of blocks to scan before cutoff
     * @param yLevelThreshold If a block is found within this Y level, the current result will be returned, if the value
     *                        is negative, then this condition doesn't apply.
     * @return The matching block positions
     */
    List<BlockPos> scanChunk(IPlayerContext ctx, BlockOptionalMetaLookup filter, ChunkPos pos, int max, int yLevelThreshold);

    /**
     * Scans a single chunk for the specified blocks.
     *
     * @param ctx             The {@link IPlayerContext} containing player and world info that the scan is based upon
     * @param blocks          The blocks to scan for
     * @param pos             The position of the target chunk
     * @param max             The maximum number of blocks to scan before cutoff
     * @param yLevelThreshold If a block is found within this Y level, the current result will be returned, if the value
     *                        is negative, then this condition doesn't apply.
     * @return The matching block positions
     */
    default List<BlockPos> scanChunk(IPlayerContext ctx, List<Block> blocks, ChunkPos pos, int max, int yLevelThreshold) {
        return scanChunk(ctx, new BlockOptionalMetaLookup(blocks), pos, max, yLevelThreshold);
    }

    /**
     * Overload of {@link #repack(IPlayerContext, int)} where the value of the {@code range} parameter is {@code 40}.
     *
     * @param ctx The player, describing the origin
     * @return The amount of chunks successfully queued for repacking
     */
    int repack(IPlayerContext ctx);

    /**
     * Queues the chunks in a square formation around the specified player, using the specified
     * range, which represents 1/2 the square's dimensions, where the player is in the center.
     *
     * @param ctx   The player, describing the origin
     * @param range The range to repack
     * @return The amount of chunks successfully queued for repacking
     */
    int repack(IPlayerContext ctx, int range);
}
