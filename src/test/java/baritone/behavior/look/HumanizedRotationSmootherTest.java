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
import static org.junit.Assert.assertTrue;

public class HumanizedRotationSmootherTest {

    private static final HumanizedRotationSmoother.Config CONFIG = new HumanizedRotationSmoother.Config(
            true,
            18.0D,
            1.0D,
            6.0D,
            0.0D,
            0.0D,
            0.0D,
            0.0D
    );

    @Test
    public void respectsMaxSpeedAndAcceleration() {
        HumanizedRotationSmoother smoother = new HumanizedRotationSmoother();
        Rotation current = new Rotation(0.0F, 0.0F);
        Rotation target = new Rotation(90.0F, 0.0F);

        Rotation first = smoother.next(current, target, CONFIG);
        assertEquals(6.0F, first.getYaw(), 0.0001F);

        Rotation second = smoother.next(first, target, CONFIG);
        assertEquals(18.0F, second.getYaw(), 0.0001F);
    }

    @Test
    public void reachesTargetOverRepeatedTicks() {
        HumanizedRotationSmoother smoother = new HumanizedRotationSmoother();
        Rotation current = new Rotation(0.0F, 0.0F);
        Rotation target = new Rotation(70.0F, 20.0F);

        for (int i = 0; i < 20; i++) {
            current = smoother.next(current, target, CONFIG);
        }

        assertEquals(target.getYaw(), current.getYaw(), 0.001F);
        assertEquals(target.getPitch(), current.getPitch(), 0.001F);
    }

    @Test
    public void wrapsYawThroughShortestPath() {
        HumanizedRotationSmoother smoother = new HumanizedRotationSmoother();
        Rotation current = new Rotation(179.0F, 0.0F);
        Rotation target = new Rotation(-179.0F, 0.0F);

        Rotation next = smoother.next(current, target, CONFIG);

        assertTrue(next.getYaw() > 179.0F || next.getYaw() < -179.0F);
        assertEquals(2.0D, HumanizedRotationSmoother.angularDistance(current, target), 0.0001D);
    }

    @Test
    public void clampsPitch() {
        HumanizedRotationSmoother smoother = new HumanizedRotationSmoother();
        HumanizedRotationSmoother.Config fast = new HumanizedRotationSmoother.Config(
                true,
                180.0D,
                180.0D,
                180.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );

        Rotation next = smoother.next(new Rotation(0.0F, 80.0F), new Rotation(0.0F, 120.0F), fast);

        assertEquals(90.0F, next.getPitch(), 0.0001F);
    }

    @Test
    public void peekDoesNotMutateVelocity() {
        HumanizedRotationSmoother smoother = new HumanizedRotationSmoother();
        Rotation current = new Rotation(0.0F, 0.0F);
        Rotation target = new Rotation(90.0F, 0.0F);

        Rotation peek = smoother.peek(current, target, CONFIG);
        Rotation next = smoother.next(current, target, CONFIG);

        assertEquals(peek.getYaw(), next.getYaw(), 0.0001F);
        assertEquals(6.0F, next.getYaw(), 0.0001F);
    }

    @Test
    public void forkIsDeterministicAndIndependent() {
        HumanizedRotationSmoother smoother = new HumanizedRotationSmoother();
        Rotation current = smoother.next(new Rotation(0.0F, 0.0F), new Rotation(90.0F, 0.0F), CONFIG);
        HumanizedRotationSmoother fork = smoother.fork();

        Rotation sourceNext = smoother.next(current, new Rotation(90.0F, 0.0F), CONFIG);
        Rotation forkNext = fork.next(current, new Rotation(90.0F, 0.0F), CONFIG);
        assertEquals(sourceNext.getYaw(), forkNext.getYaw(), 0.0001F);

        smoother.next(sourceNext, new Rotation(-90.0F, 0.0F), CONFIG);
        Rotation stillIndependent = fork.next(forkNext, new Rotation(90.0F, 0.0F), CONFIG);
        assertTrue(stillIndependent.getYaw() > forkNext.getYaw());
    }

    @Test
    public void disabledReturnsTargetImmediately() {
        HumanizedRotationSmoother smoother = new HumanizedRotationSmoother();
        HumanizedRotationSmoother.Config disabled = new HumanizedRotationSmoother.Config(
                false,
                1.0D,
                1.0D,
                1.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );

        Rotation next = smoother.next(new Rotation(0.0F, 0.0F), new Rotation(120.0F, 45.0F), disabled);

        assertEquals(120.0F, next.getYaw(), 0.0001F);
        assertEquals(45.0F, next.getPitch(), 0.0001F);
    }
}
