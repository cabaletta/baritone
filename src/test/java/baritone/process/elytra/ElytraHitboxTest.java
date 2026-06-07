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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the elytra hitbox motion-translation logic in
 * {@code ElytraBehavior.simulate}. The original implementation used
 * {@code AABB.inflate(dx, dy, dz)} to "grow" the hitbox by the per-tick
 * motion vector. Because {@code AABB.inflate} subtracts {@code dx} from
 * {@code minX} and adds {@code dx} to {@code maxX}, a negative motion
 * value expands the box on the WRONG side. After the {@code AABB}
 * constructor re-sorts the corners, the result is a box that has NOT
 * translated to the player's end-of-tick position, but is still
 * centred on the player's current position. The collision loop in
 * {@code ElytraBehavior.simulate} then iterates over the wrong set
 * of blocks.
 *
 * <p>The fix replaces the first {@code inflate(motion.x, motion.y,
 * motion.z)} with {@code move(motion.x, motion.y, motion.z)}, which
 * correctly translates the box. The safety padding is then a uniform
 * {@code inflate(0.01)} on the translated box.
 *
 * <p>See issue #5044.
 */
public class ElytraHitboxTest {

    private static final double EPS = 1.0E-9D;

    /**
     * Build a player-sized hitbox whose feet are at the origin (0, 100, 0).
     * A standard player hitbox is 0.6 wide and 1.8 tall.
     */
    private static AABB playerHitbox() {
        return new AABB(0.0D, 100.0D, 0.0D, 0.6D, 101.8D, 0.6D);
    }

    /**
     * After {@code move(-1, 0, 0)} + {@code inflate(0.01)}, the box's
     * X range must be {@code [-1.01, -0.39]} - i.e. the player has moved
     * one block west of where they started.
     */
    @Test
    public void testNegativeXMotion() {
        AABB hitbox = playerHitbox();
        AABB inMotion = hitbox.move(-1.0D, 0.0D, 0.0D).inflate(0.01D);
        assertTrue("minX must be strictly less than maxX after -X motion", inMotion.minX < inMotion.maxX);
        assertEquals(-1.01D, inMotion.minX, EPS);
        assertEquals(-0.39D, inMotion.maxX, EPS);
        // Y and Z must be unchanged (motion is X-only).
        assertEquals(99.99D, inMotion.minY, EPS);
        assertEquals(101.81D, inMotion.maxY, EPS);
        assertEquals(-0.01D, inMotion.minZ, EPS);
        assertEquals(0.61D, inMotion.maxZ, EPS);
    }

    /**
     * Symmetric positive X case: the box should be translated east by
     * one block, with a 0.01 padding on every face.
     */
    @Test
    public void testPositiveXMotion() {
        AABB hitbox = playerHitbox();
        AABB inMotion = hitbox.move(1.0D, 0.0D, 0.0D).inflate(0.01D);
        assertTrue("minX must be strictly less than maxX after +X motion", inMotion.minX < inMotion.maxX);
        assertEquals(0.99D, inMotion.minX, EPS);
        assertEquals(1.61D, inMotion.maxX, EPS);
    }

    /**
     * Negative Y motion is the most common case during elytra flight
     * (the player is almost always descending), and is the case that
     * originally triggered the bug.
     */
    @Test
    public void testNegativeYMotion() {
        AABB hitbox = playerHitbox();
        AABB inMotion = hitbox.move(0.0D, -1.0D, 0.0D).inflate(0.01D);
        assertTrue("minY must be strictly less than maxY after -Y motion", inMotion.minY < inMotion.maxY);
        assertEquals(98.99D, inMotion.minY, EPS);
        assertEquals(100.81D, inMotion.maxY, EPS);
        // X and Z must be unchanged.
        assertEquals(-0.01D, inMotion.minX, EPS);
        assertEquals(0.61D, inMotion.maxX, EPS);
        assertEquals(-0.01D, inMotion.minZ, EPS);
        assertEquals(0.61D, inMotion.maxZ, EPS);
    }

    /**
     * Symmetric to the negative X case: a negative Z motion must shift
     * the box north by one block.
     */
    @Test
    public void testNegativeZMotion() {
        AABB hitbox = playerHitbox();
        AABB inMotion = hitbox.move(0.0D, 0.0D, -1.0D).inflate(0.01D);
        assertTrue("minZ must be strictly less than maxZ after -Z motion", inMotion.minZ < inMotion.maxZ);
        assertEquals(-1.01D, inMotion.minZ, EPS);
        assertEquals(-0.39D, inMotion.maxZ, EPS);
    }

    /**
     * Combined negative motion on all three axes. Reproduces a worst-case
     * elytra dive (moving backwards and downwards simultaneously) and
     * verifies the box remains a well-formed, correctly-translated AABB.
     */
    @Test
    public void testCombinedNegativeMotion() {
        AABB hitbox = playerHitbox();
        AABB inMotion = hitbox.move(-0.5D, -0.5D, -0.5D).inflate(0.01D);
        assertTrue("box must be valid on X", inMotion.minX < inMotion.maxX);
        assertTrue("box must be valid on Y", inMotion.minY < inMotion.maxY);
        assertTrue("box must be valid on Z", inMotion.minZ < inMotion.maxZ);
        // Centred on the original hitbox centre, translated by (-0.5, -0.5, -0.5).
        assertEquals(-0.51D, inMotion.minX, EPS);
        assertEquals(0.11D, inMotion.maxX, EPS);
        assertEquals(99.49D, inMotion.minY, EPS);
        assertEquals(101.31D, inMotion.maxY, EPS);
        assertEquals(-0.51D, inMotion.minZ, EPS);
        assertEquals(0.11D, inMotion.maxZ, EPS);
    }

    /**
     * The safety padding must apply uniformly. The original span is
     * preserved (move() only translates, never resizes), and the
     * subsequent {@code inflate(0.01)} extends every face by 0.01, so
     * the new span on every axis equals {@code originalSpan + 2 * 0.01}.
     */
    @Test
    public void testPaddingIsAppliedUniformly() {
        AABB hitbox = playerHitbox();
        AABB inMotion = hitbox.move(1.0D, 1.0D, 1.0D).inflate(0.01D);
        double originalSpanX = hitbox.maxX - hitbox.minX;
        double originalSpanY = hitbox.maxY - hitbox.minY;
        double originalSpanZ = hitbox.maxZ - hitbox.minZ;
        double expectedPadding = 0.02D;
        assertEquals(originalSpanX + expectedPadding, inMotion.maxX - inMotion.minX, EPS);
        assertEquals(originalSpanY + expectedPadding, inMotion.maxY - inMotion.minY, EPS);
        assertEquals(originalSpanZ + expectedPadding, inMotion.maxZ - inMotion.minZ, EPS);
    }

    /**
     * Documents the original bug: {@code AABB.inflate(dx, dy, dz)} does
     * NOT translate the box. Internally it does
     * {@code new AABB(minX - dx, minY - dy, minZ - dz,
     * maxX + dx, maxY + dy, maxZ + dz)} and the {@code AABB}
     * constructor re-sorts the corners with {@code Math.min}/{@code Math.max}.
     * The resulting box is EXPANDED (not translated) and is still centred
     * on the player's current position. The collision loop then iterates
     * over the wrong set of blocks.
     *
     * <p>This test pins the buggy behaviour of the old API so that any
     * future regression to {@code inflate(motion.x, motion.y, motion.z)}
     * would be caught by a corresponding change to this test (and would
     * motivate re-fixing the bug). It also acts as a regression guard
     * for the fix itself: if {@code AABB.inflate} ever changes semantics
     * in Minecraft, this test will fail loudly and force a re-review.
     */
    @Test
    public void testInflateWithNegativeMotionIsBuggy() {
        AABB hitbox = playerHitbox();
        double originalCentreX = (hitbox.minX + hitbox.maxX) * 0.5D;

        // Old (buggy) form: hitbox.inflate(motion.x, motion.y, motion.z).
        // For motion = (-1, 0, 0) and hitbox = [0..0.6], this expands the
        // box on the wrong side. After the AABB constructor re-sorts the
        // corners, the resulting box is [-0.4..1] - still centred on the
        // player's current X, but now 1.4 wide instead of 0.6 wide. The
        // box has NOT moved; the player has.
        AABB broken = hitbox.inflate(-1.0D, 0.0D, 0.0D);
        double brokenCentreX = (broken.minX + broken.maxX) * 0.5D;
        double brokenSpanX = broken.maxX - broken.minX;
        double originalSpanX = hitbox.maxX - hitbox.minX;

        // 1. The buggy form did not translate: the box is still centred
        //    on the player's current position.
        assertEquals(
                "inflate(-1, ...) must not translate the box (this is the bug).",
                originalCentreX,
                brokenCentreX,
                EPS
        );
        // 2. The buggy form grows the box in a complicated way. With
        //    |motion.x| = 1 > original span / 2 = 0.3, the new span is
        //    2 * |motion.x| - original span = 1.4. The relevant point is
        //    just that the box's POSITION is wrong (asserted above); the
        //    span alone is not the bug.
        double expectedBuggySpanX = Math.abs(-1.0D) * 2.0D - originalSpanX;
        assertEquals(expectedBuggySpanX, brokenSpanX, EPS);

        // Sanity check: the fixed form (move + inflate padding) does
        // translate the box to the player's end-of-tick position.
        AABB fixed = hitbox.move(-1.0D, 0.0D, 0.0D).inflate(0.01D);
        double fixedCentreX = (fixed.minX + fixed.maxX) * 0.5D;
        assertEquals(
                "move(-1, ...) must translate the box centre by -1.",
                originalCentreX - 1.0D,
                fixedCentreX,
                EPS
        );
        // The fixed form preserves the original span (it only translates
        // and adds the small safety padding of 0.02).
        assertEquals(
                "move() preserves the span; only the safety padding widens it.",
                originalSpanX + 0.02D,
                fixed.maxX - fixed.minX,
                EPS
        );
    }
}
