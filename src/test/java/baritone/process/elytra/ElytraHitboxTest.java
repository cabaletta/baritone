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

import net.minecraft.world.phys.AABB;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests for AABB collision volume behavior in elytra flight simulation.
 * Verifies that expandTowards produces correct swept volumes for all
 * motion vector directions, preventing both tunneling (#5049) and
 * hitbox collapse (#5047).
 */
public class ElytraHitboxTest {

    /**
     * Test swept volume with positive motion vectors.
     * expandTowards(2, 0, 3) should extend the AABB in +X and +Z directions.
     */
    @Test
    public void testPositiveMotionVectors() {
        AABB hitbox = new AABB(0, 0, 0, 0.6, 1.8, 0.6);
        AABB expanded = hitbox.expandTowards(2, 0, 3);

        // Expanded box should cover start and end positions
        assertTrue("minX should be at or below start", expanded.minX <= hitbox.minX);
        assertTrue("maxX should be at or above end", expanded.maxX >= hitbox.maxX + 2);
        assertTrue("minZ should be at or below start", expanded.minZ <= hitbox.minZ);
        assertTrue("maxZ should be at or above end", expanded.maxZ >= hitbox.maxZ + 3);

        // Y should remain unchanged (zero motion)
        assertTrue("minY should be unchanged", expanded.minY == hitbox.minY);
        assertTrue("maxY should be unchanged", expanded.maxY == hitbox.maxY);

        // AABB invariants must hold
        assertTrue("minX < maxX", expanded.minX < expanded.maxX);
        assertTrue("minY < maxY", expanded.minY < expanded.maxY);
        assertTrue("minZ < maxZ", expanded.minZ < expanded.maxZ);
    }

    /**
     * Test swept volume with zero motion on one axis.
     * expandTowards(2, 0, 0) should only extend X, not Y or Z.
     */
    @Test
    public void testZeroMotionAxis() {
        AABB hitbox = new AABB(0, 0, 0, 0.6, 1.8, 0.6);
        AABB expanded = hitbox.expandTowards(2, 0, 0);

        // X should extend
        assertTrue("maxX should extend", expanded.maxX >= hitbox.maxX + 2);

        // Y and Z should remain unchanged
        assertTrue("minY unchanged", expanded.minY == hitbox.minY);
        assertTrue("maxY unchanged", expanded.maxY == hitbox.maxY);
        assertTrue("minZ unchanged", expanded.minZ == hitbox.minZ);
        assertTrue("maxZ unchanged", expanded.maxZ == hitbox.maxZ);

        // AABB invariants
        assertTrue("minX < maxX", expanded.minX < expanded.maxX);
        assertTrue("minY < maxY", expanded.minY < expanded.maxY);
        assertTrue("minZ < maxZ", expanded.minZ < expanded.maxZ);
    }

    /**
     * Test swept volume with negative motion vectors.
     * expandTowards(-2, -1, -3) should extend toward negative coordinates
     * without collapsing. This is the regression test for #5047.
     */
    @Test
    public void testNegativeMotionVectors() {
        AABB hitbox = new AABB(5, 10, 5, 5.6, 11.8, 5.6);
        AABB expanded = hitbox.expandTowards(-2, -1, -3);

        // Should extend toward negative
        assertTrue("minX should decrease", expanded.minX < hitbox.minX);
        assertTrue("minY should decrease", expanded.minY < hitbox.minY);
        assertTrue("minZ should decrease", expanded.minZ < hitbox.minZ);

        // AABB invariants MUST hold — this is the critical regression test
        assertTrue("minX < maxX (no collapse)", expanded.minX < expanded.maxX);
        assertTrue("minY < maxY (no collapse)", expanded.minY < expanded.maxY);
        assertTrue("minZ < maxZ (no collapse)", expanded.minZ < expanded.maxZ);
    }

    /**
     * Test swept volume with mixed positive/negative motion.
     * expandTowards(2, -1, 0) should handle mixed axes correctly.
     */
    @Test
    public void testMixedMotionVectors() {
        AABB hitbox = new AABB(0, 5, 0, 0.6, 6.8, 0.6);
        AABB expanded = hitbox.expandTowards(2, -1, 0);

        // X extends positive
        assertTrue("maxX extends positive", expanded.maxX >= hitbox.maxX + 2);

        // Y extends negative
        assertTrue("minY extends negative", expanded.minY < hitbox.minY);

        // Z unchanged
        assertTrue("minZ unchanged", expanded.minZ == hitbox.minZ);
        assertTrue("maxZ unchanged", expanded.maxZ == hitbox.maxZ);

        // AABB invariants
        assertTrue("minX < maxX", expanded.minX < expanded.maxX);
        assertTrue("minY < maxY", expanded.minY < expanded.maxY);
        assertTrue("minZ < maxZ", expanded.minZ < expanded.maxZ);
    }

    /**
     * Test that expandTowards with the inflate(0.01) safety padding
     * produces valid AABBs for all motion directions.
     * This matches the actual usage in ElytraBehavior.simulate().
     */
    @Test
    public void testExpandTowardsWithSafetyPadding() {
        AABB hitbox = new AABB(0, 0, 0, 0.6, 1.8, 0.6);

        // Positive motion
        AABB posMotion = hitbox.expandTowards(2, 0, 3).inflate(0.01);
        assertTrue("Positive: minX < maxX", posMotion.minX < posMotion.maxX);
        assertTrue("Positive: minY < maxY", posMotion.minY < posMotion.maxY);
        assertTrue("Positive: minZ < maxZ", posMotion.minZ < posMotion.maxZ);

        // Negative motion
        AABB negMotion = hitbox.expandTowards(-2, -1, -3).inflate(0.01);
        assertTrue("Negative: minX < maxX", negMotion.minX < negMotion.maxX);
        assertTrue("Negative: minY < maxY", negMotion.minY < negMotion.maxY);
        assertTrue("Negative: minZ < maxZ", negMotion.minZ < negMotion.maxZ);

        // Zero motion
        AABB zeroMotion = hitbox.expandTowards(0, 0, 0).inflate(0.01);
        assertTrue("Zero: minX < maxX", zeroMotion.minX < zeroMotion.maxX);
        assertTrue("Zero: minY < maxY", zeroMotion.minY < zeroMotion.maxY);
        assertTrue("Zero: minZ < maxZ", zeroMotion.minZ < zeroMotion.maxZ);
    }
}
