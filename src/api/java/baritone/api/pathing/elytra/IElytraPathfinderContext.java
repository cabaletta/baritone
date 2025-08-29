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

package baritone.api.pathing.elytra;

import baritone.api.event.events.BlockChangeEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.CompletableFuture;

public interface IElytraPathfinderContext {
    public boolean hasChunk(ChunkPos pos);
    public void queueCacheCulling(int chunkX, int chunkZ, int maxDistanceBlocks);
    public void queueForPacking(final LevelChunk chunkIn);
    public void queueBlockUpdate(BlockChangeEvent event);
    public CompletableFuture<UnpackedSegment> pathFindAsync(final BlockPos src, final BlockPos dst);
    public boolean raytrace(final double startX, final double startY, final double startZ,
                            final double endX, final double endY, final double endZ);
    public boolean raytrace(final Vec3 start, final Vec3 end);
    public boolean raytrace(final int count, final double[] src, final double[] dst, final int visibility);
    public void cancel();
    public void destroy();
    public long getSeed();

    public void RLock();
    public void RUnlock();

    public int getMaxHeight();
    public boolean passable(int x, int y, int z);

    public static final class Visibility {
        public static final int ALL = 0;
        public static final int NONE = 1;
        public static final int ANY = 2;
        private Visibility() {}
    }
}


