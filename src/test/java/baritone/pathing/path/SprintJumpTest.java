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

package baritone.pathing.path;

import org.junit.Test;

import static org.junit.Assert.*;

public class SprintJumpTest {

    // steady state ground sprint: v = (v + 0.13 * 0.98) * 0.546
    private static final double ACCEL = 0.13 * 0.98, SPRINT = ACCEL * SprintJump.GROUND_FRICTION / (1 - SprintJump.GROUND_FRICTION);

    private static Double plan(double... floors) {
        return new SprintJump(floors, 1, ACCEL).plan(0, SPRINT);
    }

    @Test
    public void fullSendOnFlatGround() {
        SprintJump jump = new SprintJump(new double[]{0, 0, 0, 0, 0, 0}, 1, ACCEL);
        assertTrue(jump.safe(jump.jump(0, SPRINT, Double.NaN)));
        assertTrue(Double.isNaN(plan(0, 0, 0, 0, 0, 0)));
        assertNull(plan(0, 0, 0)); // lands past the runway
        assertNotNull(new SprintJump(new double[]{0, 0, 0, 0, 0}, Math.sqrt(2), ACCEL).plan(0, SPRINT));
    }

    @Test
    public void stepsAndHills() {
        assertTrue(Double.isNaN(plan(0, 0, 1, 1, 1, 1)));
        assertNull(plan(0, 0, 1, 2, 2, 2)); // two steps in a row is a wall
        assertNull(plan(0, -2, -2, -2, -2, -2, -2)); // 1.25 up then 2 down is past the 3 block safe fall
    }

    @Test
    public void brakesOntoTheFirstStepOfAHill() {
        SprintJump jump = new SprintJump(new double[]{0, 0, -1, -1, -2, -2, -2, -2}, 1, ACCEL);
        double target = jump.plan(0, SPRINT);
        assertFalse(Double.isNaN(target));
        assertEquals(-1, jump.jump(0, SPRINT, target).y(), 1e-9);
    }

    @Test
    public void setsUpTheNextHopToPopAStep() {
        // full send clips the step face and landing right in front of it leaves nothing to pop it with, so brake onto
        // cell 2 and come down still moving, from where the next hop clears the step
        SprintJump jump = new SprintJump(new double[]{0, 0, 0, 0, 1, 1, 1, 1, 1, 1}, 1, ACCEL);
        assertNull(jump.jump(0, SPRINT, Double.NaN));
        assertEquals(2, jump.plan(0, SPRINT), 1e-9);
        SprintJump.Landing landing = jump.jump(0, SPRINT, 2);
        SprintJump next = new SprintJump(new double[]{0, 0, 1, 1, 1, 1, 1, 1}, 1, ACCEL);
        assertTrue(next.safe(next.jump(landing.s() - 2, landing.v(), Double.NaN)));
    }
}
