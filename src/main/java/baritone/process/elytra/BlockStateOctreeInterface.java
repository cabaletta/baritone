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

package baritone.process.elytra;

import baritone.process.elytra.pathfinder.Chunk;
import baritone.process.elytra.pathfinder.NetherPathfinder;

/**
 * @author Brady
 */
public final class BlockStateOctreeInterface {

    private final NetherPathfinderContext context;
    private final NetherPathfinder pathfinder;
    private final int minY;
    // The chunk the last lookup fell in, so that a run of lookups inside one chunk costs one table
    // lookup. Cleared under the write lock by whatever replaces or culls chunks.
    Chunk chunk;

    // Guarantee that the first lookup will fetch the context by setting MAX_VALUE
    private int prevChunkX = Integer.MAX_VALUE;
    private int prevChunkZ = Integer.MAX_VALUE;

    public BlockStateOctreeInterface(final NetherPathfinderContext context) {
        this.context = context;
        this.pathfinder = context.context;
        this.minY = context.minY;
    }

    public boolean get0(final int x, final int y, final int z) {
        final int adjustedY = y - this.minY;
        if (adjustedY < 0 || adjustedY > 383) {
            return false;
        }
        final int chunkX = x >> 4;
        final int chunkZ = z >> 4;
        if (this.chunk == null | ((chunkX ^ this.prevChunkX) | (chunkZ ^ this.prevChunkZ)) != 0) {
            this.prevChunkX = chunkX;
            this.prevChunkZ = chunkZ;
            this.chunk = this.pathfinder.getChunkOrDefault(chunkX, chunkZ, true);
        }
        return this.chunk.isSolid(x & 0xF, adjustedY, z & 0xF);
    }
}
