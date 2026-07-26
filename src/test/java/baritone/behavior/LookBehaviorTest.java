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

package baritone.behavior;

import baritone.api.utils.RotationUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LookBehaviorTest {

    @Test
    public void distanceScalingPreservesWorldOffset() {
        double originalAngle = 1.0D;
        double targetDistance = 4.5D;
        double scaledAngle = LookBehavior.distanceScaledAngle(originalAngle, targetDistance);

        double originalOffset = Math.tan(originalAngle * RotationUtils.DEG_TO_RAD);
        double scaledOffset = Math.tan(scaledAngle * RotationUtils.DEG_TO_RAD) * targetDistance;

        assertTrue(Math.abs(scaledAngle) < Math.abs(originalAngle));
        assertEquals(originalOffset, scaledOffset, 1.0E-12D);
        assertEquals(-scaledAngle, LookBehavior.distanceScaledAngle(-originalAngle, targetDistance), 1.0E-12D);
    }

    @Test
    public void distanceScalingDoesNotAmplifyCloseOrUnknownTargets() {
        double angle = 1.0D;

        assertEquals(angle, LookBehavior.distanceScaledAngle(angle, 1.0D), 0.0D);
        assertEquals(angle, LookBehavior.distanceScaledAngle(angle, 0.5D), 0.0D);
        assertEquals(angle, LookBehavior.distanceScaledAngle(angle, Double.NaN), 0.0D);
        assertEquals(0.0D, LookBehavior.distanceScaledAngle(0.0D, 4.5D), 0.0D);
    }
}
