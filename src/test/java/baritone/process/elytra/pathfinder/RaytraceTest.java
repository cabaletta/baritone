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

package baritone.process.elytra.pathfinder;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RaytraceTest {

    @Test
    public void everyOracleRayAgreesWithTheNativeRaytracer() throws Exception {
        final List<Oracle.Ray> rays = Oracle.rays();
        assertEquals(4000, rays.size());
        final NetherPathfinder ctx = Oracle.generatedWorld(16);
        final double[] where = new double[3];
        int wrongHit = 0;
        int wrongWhere = 0;
        for (Oracle.Ray r : rays) {
            final boolean hit = Raytracer.raytrace(ctx, r.from[0], r.from[1], r.from[2], r.to[0], r.to[1], r.to[2], NetherPathfinder.CACHE_MISS_SOLID, where, 0);
            if (hit != r.hit) {
                wrongHit++;
            } else if (hit && (where[0] != r.where[0] || where[1] != r.where[1] || where[2] != r.where[2])) {
                wrongWhere++;
            }
        }
        assertEquals("rays that hit where the native raytracer missed, or the other way round", 0, wrongHit);
        assertEquals("hits at a different position than the native raytracer's", 0, wrongWhere);
    }

    @Test
    public void nothingToHitInAirMode() {
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NetherPathfinder.DIMENSION_NETHER, 128);
        assertTrue(ctx.isVisible(NetherPathfinder.CACHE_MISS_AIR, 0.5, 64.5, 0.5, 300.5, 20.5, -200.5));
    }

    @Test
    public void unknownChunksAreWallsInSolidMode() {
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NetherPathfinder.DIMENSION_NETHER, 128);
        final double[] hitPos = new double[3];
        final boolean[] hits = new boolean[1];
        ctx.raytrace(NetherPathfinder.CACHE_MISS_SOLID, 1, new double[]{0.5, 64.5, 0.5}, new double[]{30.5, 64.5, 0.5}, hits, hitPos);
        assertTrue(hits[0]);
        // the origin is inside the solid, so the hit is the origin
        assertEquals(0.5, hitPos[0], 0);
        assertEquals(64.5, hitPos[1], 0);
        assertEquals(0.5, hitPos[2], 0);
    }

    @Test
    public void hitsTheFaceOfABlock() {
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NetherPathfinder.DIMENSION_NETHER, 128);
        final Chunk chunk = ctx.allocateAndInsertChunk(0, 0);
        chunk.setBlock(8, 64, 8, true);
        final double[] hitPos = new double[3];
        final boolean[] hits = new boolean[1];
        ctx.raytrace(NetherPathfinder.CACHE_MISS_AIR, 1, new double[]{2.5, 64.5, 8.5}, new double[]{14.5, 64.5, 8.5}, hits, hitPos);
        assertTrue(hits[0]);
        assertEquals(8.0, hitPos[0], 1e-9);
        assertEquals(64.5, hitPos[1], 1e-9);
        assertEquals(8.5, hitPos[2], 1e-9);
        // and from the other side
        ctx.raytrace(NetherPathfinder.CACHE_MISS_AIR, 1, new double[]{14.5, 64.5, 8.5}, new double[]{2.5, 64.5, 8.5}, hits, hitPos);
        assertTrue(hits[0]);
        assertEquals(9.0, hitPos[0], 1e-9);
        // a ray that stops short of it
        assertTrue(ctx.isVisible(NetherPathfinder.CACHE_MISS_AIR, 2.5, 64.5, 8.5, 7.5, 64.5, 8.5));
        // a ray past it in another row
        assertTrue(ctx.isVisible(NetherPathfinder.CACHE_MISS_AIR, 2.5, 64.5, 9.5, 14.5, 64.5, 9.5));
    }

    @Test
    public void crossesChunkAndSlabBoundaries() {
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NetherPathfinder.DIMENSION_NETHER, 128);
        ctx.allocateAndInsertChunk(0, 0);
        ctx.allocateAndInsertChunk(1, 0);
        ctx.allocateAndInsertChunk(2, 0).setBlock(3, 70, 4, true);
        assertTrue(ctx.isVisible(NetherPathfinder.CACHE_MISS_AIR, 1.5, 40.5, 4.5, 34.5, 70.5, 4.5));
        assertFalse(ctx.isVisible(NetherPathfinder.CACHE_MISS_AIR, 1.5, 40.5, 4.5, 36.5, 70.5, 4.5));
        assertFalse(ctx.isVisible(NetherPathfinder.CACHE_MISS_AIR, 36.5, 70.5, 4.5, 1.5, 40.5, 4.5));
    }

    @Test
    public void theRayThatEndedTheNativeProcessTerminates() {
        // nether-pathfinder issue 23: an end exactly on a chunk corner
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NetherPathfinder.DIMENSION_NETHER, 128);
        assertTrue(ctx.isVisible(NetherPathfinder.CACHE_MISS_AIR, 386.7112215521066, 137.40911926818373, 7.0554159455922285, 416.0, 142.0, 0.0));
        assertFalse(ctx.isVisible(NetherPathfinder.CACHE_MISS_SOLID, 386.7112215521066, 137.40911926818373, 7.0554159455922285, 416.0, 142.0, 0.0));
    }

    @Test
    public void isVisibleMultiReportsTheFirstSegmentThatDecides() {
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NetherPathfinder.DIMENSION_NETHER, 128);
        ctx.allocateAndInsertChunk(0, 0).setBlock(8, 64, 8, true);
        final double[] start = {2.5, 64.5, 8.5, 2.5, 64.5, 9.5, 2.5, 64.5, 8.5};
        final double[] end = {14.5, 64.5, 8.5, 14.5, 64.5, 9.5, 14.5, 64.5, 8.5};
        // first clear one is index 1; first blocked one is index 0
        assertEquals(1, ctx.isVisibleMulti(NetherPathfinder.CACHE_MISS_AIR, 3, start, end, true));
        assertEquals(0, ctx.isVisibleMulti(NetherPathfinder.CACHE_MISS_AIR, 3, start, end, false));
        // all clear: -1 in "all" mode; none blocked
        final double[] clearEnd = {7.5, 64.5, 8.5, 14.5, 64.5, 9.5, 7.5, 64.5, 8.5};
        assertEquals(-1, ctx.isVisibleMulti(NetherPathfinder.CACHE_MISS_AIR, 3, start, clearEnd, false));
        assertEquals(0, ctx.isVisibleMulti(NetherPathfinder.CACHE_MISS_AIR, 3, start, clearEnd, true));
    }
}
