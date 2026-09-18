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

import net.minecraft.world.phys.Vec3;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RaytraceTest {

    @Test
    public void everyOracleRayAgreesWithTheNativeRaytracer() throws Exception {
        final List<Oracle.Ray> rays = Oracle.rays();
        assertEquals(4000, rays.size());
        final NetherPathfinder ctx = Oracle.generatedWorld(16);
        int wrongHit = 0;
        int wrongWhere = 0;
        for (Oracle.Ray r : rays) {
            final Vec3 hit = Raytracer.raytrace(ctx, r.from[0], r.from[1], r.from[2], r.to[0], r.to[1], r.to[2], NetherPathfinder.CacheMiss.SOLID);
            if ((hit != null) != r.hit) {
                wrongHit++;
            } else if (hit != null && (hit.x != r.where[0] || hit.y != r.where[1] || hit.z != r.where[2])) {
                wrongWhere++;
            }
        }
        assertEquals("rays that hit where the native raytracer missed, or the other way round", 0, wrongHit);
        assertEquals("hits at a different position than the native raytracer's", 0, wrongWhere);
    }

    @Test
    public void nothingToHitInAirMode() {
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NetherPathfinder.Dimension.NETHER, 128);
        assertTrue(visible(ctx, NetherPathfinder.CacheMiss.AIR, 0.5, 64.5, 0.5, 300.5, 20.5, -200.5));
    }

    @Test
    public void unknownChunksAreWallsInSolidMode() {
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NetherPathfinder.Dimension.NETHER, 128);
        final Vec3 hit = Raytracer.raytrace(ctx, 0.5, 64.5, 0.5, 30.5, 64.5, 0.5, NetherPathfinder.CacheMiss.SOLID);
        assertNotNull(hit);
        // the origin is inside the solid, so the hit is the origin
        assertEquals(0.5, hit.x, 0);
        assertEquals(64.5, hit.y, 0);
        assertEquals(0.5, hit.z, 0);
    }

    @Test
    public void hitsTheFaceOfABlock() {
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NetherPathfinder.Dimension.NETHER, 128);
        final Chunk chunk = ctx.allocateAndInsertChunk(0, 0);
        chunk.setBlock(8, 64, 8, true);
        Vec3 hit = Raytracer.raytrace(ctx, 2.5, 64.5, 8.5, 14.5, 64.5, 8.5, NetherPathfinder.CacheMiss.AIR);
        assertNotNull(hit);
        assertEquals(8.0, hit.x, 1e-9);
        assertEquals(64.5, hit.y, 1e-9);
        assertEquals(8.5, hit.z, 1e-9);
        // and from the other side
        hit = Raytracer.raytrace(ctx, 14.5, 64.5, 8.5, 2.5, 64.5, 8.5, NetherPathfinder.CacheMiss.AIR);
        assertNotNull(hit);
        assertEquals(9.0, hit.x, 1e-9);
        // a ray that stops short of it
        assertTrue(visible(ctx, NetherPathfinder.CacheMiss.AIR, 2.5, 64.5, 8.5, 7.5, 64.5, 8.5));
        // a ray past it in another row
        assertTrue(visible(ctx, NetherPathfinder.CacheMiss.AIR, 2.5, 64.5, 9.5, 14.5, 64.5, 9.5));
    }

    @Test
    public void crossesChunkAndSlabBoundaries() {
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NetherPathfinder.Dimension.NETHER, 128);
        ctx.allocateAndInsertChunk(0, 0);
        ctx.allocateAndInsertChunk(1, 0);
        ctx.allocateAndInsertChunk(2, 0).setBlock(3, 70, 4, true);
        assertTrue(visible(ctx, NetherPathfinder.CacheMiss.AIR, 1.5, 40.5, 4.5, 34.5, 70.5, 4.5));
        assertFalse(visible(ctx, NetherPathfinder.CacheMiss.AIR, 1.5, 40.5, 4.5, 36.5, 70.5, 4.5));
        assertFalse(visible(ctx, NetherPathfinder.CacheMiss.AIR, 36.5, 70.5, 4.5, 1.5, 40.5, 4.5));
    }

    @Test
    public void theRayThatEndedTheNativeProcessTerminates() {
        // nether-pathfinder issue 23: an end exactly on a chunk corner
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NetherPathfinder.Dimension.NETHER, 128);
        assertTrue(visible(ctx, NetherPathfinder.CacheMiss.AIR, 386.7112215521066, 137.40911926818373, 7.0554159455922285, 416.0, 142.0, 0.0));
        assertFalse(visible(ctx, NetherPathfinder.CacheMiss.SOLID, 386.7112215521066, 137.40911926818373, 7.0554159455922285, 416.0, 142.0, 0.0));
    }

    @Test
    public void eachRayAnswersOnItsOwn() {
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NetherPathfinder.Dimension.NETHER, 128);
        ctx.allocateAndInsertChunk(0, 0).setBlock(8, 64, 8, true);
        // through the block, past it in another row, and stopping short of it
        assertFalse(visible(ctx, NetherPathfinder.CacheMiss.AIR, 2.5, 64.5, 8.5, 14.5, 64.5, 8.5));
        assertTrue(visible(ctx, NetherPathfinder.CacheMiss.AIR, 2.5, 64.5, 9.5, 14.5, 64.5, 9.5));
        assertTrue(visible(ctx, NetherPathfinder.CacheMiss.AIR, 2.5, 64.5, 8.5, 7.5, 64.5, 8.5));
    }
    /** Whether the ray reaches its end: the pathfinder's old isVisible, one ray at a time now. */
    private static boolean visible(NetherPathfinder ctx, NetherPathfinder.CacheMiss mode, double x1, double y1, double z1, double x2, double y2, double z2) {
        return Raytracer.raytrace(ctx, x1, y1, z1, x2, y2, z2, mode) == null;
    }
}
