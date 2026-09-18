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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The rays the native library could not take: a segment of no length and a coordinate that is not
 * a finite number made it exit the process or walk for ever, and a segment ending exactly on a
 * voxel boundary sometimes did. Each test has a timeout because the failure mode is a hang.
 */
public class DegenerateRaysTest {

    private static final NetherPathfinder.CacheMiss SOLID = NetherPathfinder.CacheMiss.SOLID;

    /** Chunk (0, 0) from the game: section 3 (y 48 to 63) all solid, and one block at (5, 70, 5). */
    private static NetherPathfinder world() {
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NetherPathfinder.Dimension.NETHER, 128);
        final Chunk chunk = ctx.allocateAndInsertChunk(0, 0);
        chunk.fillSection(3, true);
        chunk.setBlock(5, 70, 5, true);
        return ctx;
    }

    @Test(timeout = 2000)
    public void aPointInTheAirIsVisibleFromItself() {
        final NetherPathfinder ctx = world();
        assertTrue(visible(ctx, SOLID, 3.5, 71.5, 3.5, 3.5, 71.5, 3.5));
        assertNull(Raytracer.raytrace(ctx, 3.5, 71.5, 3.5, 3.5, 71.5, 3.5, SOLID));
    }

    @Test(timeout = 2000)
    public void aPointInsideABlockIsAHitAtItself() {
        final NetherPathfinder ctx = world();
        assertFalse(visible(ctx, SOLID, 5.5, 70.5, 5.5, 5.5, 70.5, 5.5));
        final Vec3 hit = Raytracer.raytrace(ctx, 5.25, 70.5, 5.75, 5.25, 70.5, 5.75, SOLID);
        assertNotNull(hit);
        assertEquals(5.25, hit.x, 0);
        assertEquals(70.5, hit.y, 0);
        assertEquals(5.75, hit.z, 0);
        // a point outside the world is in the air, as a ray there is
        assertTrue(visible(ctx, SOLID, 5.5, 400.0, 5.5, 5.5, 400.0, 5.5));
    }

    @Test(timeout = 2000)
    public void aCoordinateThatIsNotFiniteIsRefused() {
        final NetherPathfinder ctx = world();
        final double[] bad = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double v : bad) {
            for (int axis = 0; axis < 6; axis++) {
                final double[] p = {3.5, 71.5, 3.5, 8.5, 71.5, 8.5};
                p[axis] = v;
                try {
                    visible(ctx, SOLID, p[0], p[1], p[2], p[3], p[4], p[5]);
                    throw new AssertionError("accepted " + v + " at " + axis);
                } catch (IllegalArgumentException expected) {
                    // good
                }
            }
        }
        // both ends infinite: the length is NaN rather than infinite
        try {
            visible(ctx, SOLID, Double.POSITIVE_INFINITY, 71.5, 3.5, Double.POSITIVE_INFINITY, 71.5, 8.5);
            throw new AssertionError("accepted two infinities");
        } catch (IllegalArgumentException expected) {
            // good
        }
    }

    @Test(timeout = 5000)
    public void aRayEndingOnAVoxelCornerInTheAirAnswersLikeOneEndingJustInside() {
        // Every node of a flight path is a corner inside its air cube, like (8, 68, 8) here, four
        // blocks above the solid section and clear of the block at (5, 70, 5).
        final NetherPathfinder ctx = world();
        int checked = 0;
        for (double ox = 0.3; ox < 16; ox += 1.7) {
            for (double oz = 0.7; oz < 16; oz += 1.9) {
                for (double oy = 40.2; oy < 100; oy += 4.9) {
                    final boolean exact = visible(ctx, SOLID, ox, oy, oz, 8.0, 68.0, 8.0);
                    final double nudge = 1e-6;
                    final boolean inside = visible(ctx, SOLID, ox, oy, oz,
                            8.0 + (ox < 8.0 ? -nudge : nudge), 68.0 + (oy < 68.0 ? -nudge : nudge), 8.0 + (oz < 8.0 ? -nudge : nudge));
                    assertEquals("from " + ox + ", " + oy + ", " + oz, inside, exact);
                    checked++;
                }
            }
        }
        assertTrue(checked > 500);
    }

    @Test(timeout = 5000)
    public void aRayEndingOnACornerOfASolidBlockAnswers() {
        // (8, 64, 8) is on the solid section's top face. Whether a ray that ends there grazes the
        // section is a tie that rounding settles either way; what matters is that it answers.
        // The native library sometimes stepped into a node the ray never entered and exited.
        final NetherPathfinder ctx = world();
        int blocked = 0, clear = 0;
        for (double ox = 0.3; ox < 16; ox += 1.7) {
            for (double oz = 0.7; oz < 16; oz += 1.9) {
                for (double oy = 64.2; oy < 100; oy += 4.9) {
                    if (visible(ctx, SOLID, ox, oy, oz, 8.0, 64.0, 8.0)) clear++; else blocked++;
                }
            }
        }
        assertTrue("some answers of each kind: " + clear + " clear, " + blocked + " blocked", clear > 0 && clear + blocked > 300);
    }

    @Test(timeout = 2000)
    public void aPointAmongOtherRaysIsAnsweredLikeAnyOther() {
        final NetherPathfinder ctx = world();
        // a point, a ray through the block, a clear ray
        assertNull(Raytracer.raytrace(ctx, 3.5, 71.5, 3.5, 3.5, 71.5, 3.5, SOLID));
        assertNotNull(Raytracer.raytrace(ctx, 2.5, 70.5, 5.5, 8.5, 70.5, 5.5, SOLID));
        assertNull(Raytracer.raytrace(ctx, 2.5, 75.5, 2.5, 8.5, 75.5, 8.5, SOLID));
    }
    /** Whether the ray reaches its end: the pathfinder's old isVisible, one ray at a time now. */
    private static boolean visible(NetherPathfinder ctx, NetherPathfinder.CacheMiss mode, double x1, double y1, double z1, double x2, double y2, double z2) {
        return Raytracer.raytrace(ctx, x1, y1, z1, x2, y2, z2, mode) == null;
    }
}
