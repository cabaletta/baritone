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

import baritone.Baritone;
import baritone.api.behavior.look.ITickableAimProcessor;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.Rotation;

public final class AimProcessor implements ITickableAimProcessor {

    private final IPlayerContext ctx;
    private final ForkableRandom rand;
    private final HumanizedRotationSmoother smoother;
    private double randomYawOffset;
    private double randomPitchOffset;
    private Rotation simulatedPrevRotation;

    public AimProcessor(IPlayerContext ctx) {
        this.ctx = ctx;
        this.rand = new ForkableRandom();
        this.smoother = new HumanizedRotationSmoother();
        this.tick();
    }

    private AimProcessor(AimProcessor source) {
        this.ctx = source.ctx;
        this.rand = source.rand.fork();
        this.smoother = source.smoother.fork();
        this.randomYawOffset = source.randomYawOffset;
        this.randomPitchOffset = source.randomPitchOffset;
        this.simulatedPrevRotation = source.getPrevRotation();
    }

    @Override
    public Rotation peekRotation(final Rotation rotation) {
        return this.calculate(rotation, false, false, true);
    }

    @Override
    public Rotation peekRotationForReachability(Rotation desired) {
        return this.calculate(desired, false, true, true);
    }

    @Override
    public void tick() {
        this.randomYawOffset = (this.rand.nextDouble() - 0.5D) * Baritone.settings().randomLooking.value;
        this.randomPitchOffset = (this.rand.nextDouble() - 0.5D) * Baritone.settings().randomLooking.value;

        double random = this.rand.nextDouble() - 0.5D;
        if (Math.abs(random) < 0.1D) {
            random *= 4.0D;
        }
        this.randomYawOffset += random * Baritone.settings().randomLooking113.value;
    }

    @Override
    public void advance(int ticks) {
        for (int i = 0; i < ticks; i++) {
            this.tick();
        }
    }

    @Override
    public Rotation nextRotation(final Rotation rotation) {
        Rotation actual = this.calculate(rotation, true, false, true);
        if (this.simulatedPrevRotation != null) {
            this.simulatedPrevRotation = actual;
        }
        this.tick();
        return actual;
    }

    public Rotation nextRotationWithoutHumanizer(Rotation rotation) {
        Rotation actual = this.calculate(rotation, false, true, true);
        if (this.simulatedPrevRotation != null) {
            this.simulatedPrevRotation = actual;
        }
        this.tick();
        return actual;
    }

    @Override
    public ITickableAimProcessor fork() {
        return new AimProcessor(this);
    }

    private Rotation calculate(Rotation rotation, boolean mutate, boolean reachability, boolean quantize) {
        Rotation prev = this.getPrevRotation();

        float desiredYaw = rotation.getYaw();
        float desiredPitch = rotation.getPitch();

        if (desiredPitch == prev.getPitch()) {
            desiredPitch = nudgeToLevel(desiredPitch);
        }

        desiredYaw += this.randomYawOffset;
        desiredPitch += this.randomPitchOffset;

        Rotation desired = new Rotation(desiredYaw, desiredPitch).normalizeAndClamp();
        Rotation aimed = reachability ? desired : this.humanized(prev, desired, mutate);
        if (!quantize) {
            return aimed;
        }
        return new Rotation(
                this.calculateMouseMove(prev.getYaw(), aimed.getYaw()),
                this.calculateMouseMove(prev.getPitch(), aimed.getPitch())
        ).normalizeAndClamp();
    }

    private Rotation humanized(Rotation prev, Rotation desired, boolean mutate) {
        HumanizedRotationSmoother.Config config = new HumanizedRotationSmoother.Config(
                Baritone.settings().humanizeLook.value,
                this.ctx.player().isFallFlying()
                        ? Baritone.settings().humanizeLookElytraMaxDegreesPerTick.value
                        : Baritone.settings().humanizeLookMaxDegreesPerTick.value,
                Baritone.settings().humanizeLookMinDegreesPerTick.value,
                Baritone.settings().humanizeLookAcceleration.value,
                Baritone.settings().humanizeLookJitter.value,
                0.0D,
                Baritone.settings().humanizeLookOvershootChance.value,
                Baritone.settings().humanizeLookMaxOvershoot.value
        );
        return mutate ? this.smoother.next(prev, desired, config) : this.smoother.peek(prev, desired, config);
    }

    private Rotation getPrevRotation() {
        return this.simulatedPrevRotation != null ? this.simulatedPrevRotation : this.ctx.playerRotations();
    }

    private static float nudgeToLevel(float pitch) {
        if (pitch < -20.0F) {
            return pitch + 1.0F;
        } else if (pitch > 10.0F) {
            return pitch - 1.0F;
        }
        return pitch;
    }

    private float calculateMouseMove(float current, float target) {
        final float delta = target - current;
        final double deltaPx = this.angleToMouse(delta);
        return current + this.mouseToAngle(deltaPx);
    }

    private double angleToMouse(float angleDelta) {
        final float minAngleChange = this.mouseToAngle(1);
        return Math.round(angleDelta / minAngleChange);
    }

    private float mouseToAngle(double mouseDelta) {
        final double f = this.ctx.minecraft().options.sensitivity().get() * (double) 0.6f + (double) 0.2f;
        return (float) (mouseDelta * f * f * f * 8.0d) * 0.15f;
    }
}
