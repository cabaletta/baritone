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

package baritone.utils;

import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.RotationUtils.RotationArc;
import net.minecraft.world.phys.Vec3;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RotationUtilsTest {

    private static final double EPSILON = 1.0E-10;

    @Test
    public void testAlerpIdenticalPoints() {
        Vec3 point = new Vec3(1, 2, 3);

        assertVecEquals(point, RotationUtils.alerp(point, point, Vec3.ZERO, 0.5));
    }

    @Test
    public void testAlerpZeroVectorFallsBackToLine() {
        assertVecEquals(
                new Vec3(0.5, 0, 0),
                RotationUtils.alerp(Vec3.ZERO, new Vec3(2, 0, 0), Vec3.ZERO, 0.25));
    }

    @Test
    public void testAlerpSameRayFallsBackToLine() {
        assertVecEquals(
                new Vec3(1.5, 0, 0),
                RotationUtils.alerp(new Vec3(1, 0, 0), new Vec3(2, 0, 0), Vec3.ZERO, 0.5));
    }

    @Test
    public void testAlerpOppositeRayUsesStableHalfCircle() {
        assertVecEquals(
                new Vec3(0, 0, 1),
                RotationUtils.alerp(new Vec3(1, 0, 0), new Vec3(-1, 0, 0), Vec3.ZERO, 0.5));
    }

    @Test
    public void testRotationArcUsesAngularSpeed() {
        assertEquals(
                1,
                countTicks(RotationArc.fromAngularSpeed(new Rotation(0, 0), new Rotation(10, 0), 30)));
        assertEquals(
                3,
                countTicks(RotationArc.fromAngularSpeed(new Rotation(0, 0), new Rotation(90, 0), 30)));
        assertEquals(
                4,
                countTicks(RotationArc.fromAngularSpeed(new Rotation(0, 0), new Rotation(91, 0), 30)));
    }

    @Test
    public void testRotationArcAvoidsViewPole() {
        RotationArc arc = new RotationArc(new Rotation(0, 80), new Rotation(180, 80), 10);

        assertEquals(90, arc.arcAt(0.5).getYaw(), 1.0E-4);
        assertEquals(80, arc.arcAt(0.5).getPitch(), 1.0E-4);
    }

    private static void assertVecEquals(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x, actual.x, EPSILON);
        assertEquals(expected.y, actual.y, EPSILON);
        assertEquals(expected.z, actual.z, EPSILON);
    }

    private static int countTicks(RotationArc arc) {
        int ticks = 0;
        while (!arc.isComplete()) {
            arc.advance();
            ticks++;
        }
        return ticks;
    }
}
