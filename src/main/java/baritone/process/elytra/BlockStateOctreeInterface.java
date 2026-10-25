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
    PrevChunk cached;

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
        PrevChunk cached = this.cached;
        final Chunk chunk;
        if (cached == null || ((chunkX ^ cached.x()) | (chunkZ ^ cached.z())) != 0) {
            chunk = this.pathfinder.getChunkOrDefault(chunkX, chunkZ, true);
            this.cached = new PrevChunk(chunk, chunkX, chunkZ);
        } else {
            chunk = cached.chunk();
        }
        return chunk.isSolid(x & 0xF, adjustedY, z & 0xF);
    }

    private static record PrevChunk(Chunk chunk, int x, int z) {}
}
