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

package baritone.behavior.look;

import baritone.api.utils.Rotation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VisualRotationHelperTest {

    @Test
    public void interpolatesYawAcrossBoundaryByShortestPath() {
        Rotation interpolated = VisualRotationHelper.interpolate(
                new Rotation(170.0F, 0.0F),
                new Rotation(-170.0F, 0.0F),
                0.5F
        );

        assertEquals(180.0F, Math.abs(interpolated.getYaw()), 0.0001F);
    }

    @Test
    public void interpolationIsContinuous() {
        Rotation from = new Rotation(10.0F, -10.0F);
        Rotation to = new Rotation(50.0F, 30.0F);

        Rotation start = VisualRotationHelper.interpolate(from, to, 0.0F);
        Rotation middle = VisualRotationHelper.interpolate(from, to, 0.5F);
        Rotation end = VisualRotationHelper.interpolate(from, to, 1.0F);

        assertEquals(10.0F, start.getYaw(), 0.0001F);
        assertEquals(30.0F, middle.getYaw(), 0.0001F);
        assertEquals(50.0F, end.getYaw(), 0.0001F);
        assertEquals(10.0F, middle.getPitch(), 0.0001F);
    }

    @Test
    public void movementPostureKeepsHeadingYawAndReplacesPitch() {
        Rotation posture = VisualRotationHelper.movementPosture(new Rotation(42.0F, -70.0F), 10.0D);

        assertEquals(42.0F, posture.getYaw(), 0.0001F);
        assertEquals(10.0F, posture.getPitch(), 0.0001F);
    }

    @Test
    public void movementPostureClampsPitch() {
        Rotation posture = VisualRotationHelper.movementPosture(new Rotation(42.0F, 0.0F), 120.0D);

        assertEquals(90.0F, posture.getPitch(), 0.0001F);
    }
}
