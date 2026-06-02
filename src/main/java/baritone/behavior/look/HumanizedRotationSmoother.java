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

public final class HumanizedRotationSmoother {

    private double yawVelocity;
    private double pitchVelocity;

    public Rotation peek(Rotation current, Rotation target, Config config) {
        return this.step(current, target, config, false);
    }

    public Rotation next(Rotation current, Rotation target, Config config) {
        return this.step(current, target, config, true);
    }

    public HumanizedRotationSmoother fork() {
        HumanizedRotationSmoother fork = new HumanizedRotationSmoother();
        fork.yawVelocity = this.yawVelocity;
        fork.pitchVelocity = this.pitchVelocity;
        return fork;
    }

    private Rotation step(Rotation current, Rotation target, Config config, boolean mutate) {
        Rotation clampedTarget = target.normalizeAndClamp();
        if (!config.enabled) {
            if (mutate) {
                this.yawVelocity = 0.0D;
                this.pitchVelocity = 0.0D;
            }
            return clampedTarget;
        }

        Rotation adjustedTarget = this.adjustTarget(current, clampedTarget, config);
        AxisStep yaw = this.axisStep(
                Rotation.normalizeYaw(adjustedTarget.getYaw() - current.getYaw()),
                this.yawVelocity,
                config
        );
        AxisStep pitch = this.axisStep(
                adjustedTarget.getPitch() - current.getPitch(),
                this.pitchVelocity,
                config
        );

        if (mutate) {
            this.yawVelocity = yaw.velocity;
            this.pitchVelocity = pitch.velocity;
        }

        return new Rotation(
                current.getYaw() + (float) yaw.delta,
                current.getPitch() + (float) pitch.delta
        ).normalizeAndClamp();
    }

    private Rotation adjustTarget(Rotation current, Rotation target, Config config) {
        double yawDelta = Rotation.normalizeYaw(target.getYaw() - current.getYaw());
        double pitchDelta = target.getPitch() - current.getPitch();
        double distance = Math.hypot(yawDelta, pitchDelta);

        double jitterYaw = 0.0D;
        double jitterPitch = 0.0D;
        if (config.jitter > 0.0D && distance > 0.01D) {
            jitterYaw = deterministicNoise(current.getYaw(), target.getYaw(), 11.0D) * config.jitter;
            jitterPitch = deterministicNoise(current.getPitch(), target.getPitch(), 29.0D) * config.jitter;
        }

        double overshootYaw = 0.0D;
        double overshootPitch = 0.0D;
        if (config.overshootChance > 0.0D && config.maxOvershoot > 0.0D && distance > 12.0D) {
            double chance = (deterministicNoise(target.getYaw(), target.getPitch(), 47.0D) + 1.0D) * 0.5D;
            if (chance < clamp(config.overshootChance, 0.0D, 1.0D)) {
                double amount = config.maxOvershoot * clamp(distance / 90.0D, 0.0D, 1.0D);
                overshootYaw = Math.signum(yawDelta) * amount;
                overshootPitch = Math.signum(pitchDelta) * amount;
            }
        }

        return new Rotation(
                target.getYaw() + (float) (jitterYaw + overshootYaw),
                target.getPitch() + (float) (jitterPitch + overshootPitch)
        ).normalizeAndClamp();
    }

    private AxisStep axisStep(double delta, double velocity, Config config) {
        double distance = Math.abs(delta);
        if (distance < 0.0001D) {
            return new AxisStep(0.0D, 0.0D);
        }

        double requested = Math.signum(delta) * this.requestedSpeed(distance, config);
        double acceleration = Math.max(0.0D, config.acceleration);
        double nextVelocity;
        if (acceleration == 0.0D) {
            nextVelocity = requested;
        } else {
            nextVelocity = velocity + clamp(requested - velocity, -acceleration, acceleration);
        }
        nextVelocity = clamp(nextVelocity, -config.maxDegreesPerTick, config.maxDegreesPerTick);

        double step = Math.signum(delta) * Math.min(Math.abs(nextVelocity), distance);
        return new AxisStep(step, step);
    }

    private double requestedSpeed(double distance, Config config) {
        if (config.maxDegreesPerTick <= config.minDegreesPerTick) {
            return config.maxDegreesPerTick;
        }
        double t = clamp(distance / 45.0D, 0.0D, 1.0D);
        double eased = t * t * (3.0D - 2.0D * t);
        return config.minDegreesPerTick + (config.maxDegreesPerTick - config.minDegreesPerTick) * eased;
    }

    public static double angularDistance(Rotation a, Rotation b) {
        double yaw = Rotation.normalizeYaw(b.getYaw() - a.getYaw());
        double pitch = b.getPitch() - a.getPitch();
        return Math.hypot(yaw, pitch);
    }

    private static double deterministicNoise(double a, double b, double salt) {
        double value = Math.sin(a * 12.9898D + b * 78.233D + salt * 37.719D) * 43758.5453D;
        return (value - Math.floor(value)) * 2.0D - 1.0D;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class AxisStep {
        private final double delta;
        private final double velocity;

        private AxisStep(double delta, double velocity) {
            this.delta = delta;
            this.velocity = velocity;
        }
    }

    public static final class Config {
        public final boolean enabled;
        public final double maxDegreesPerTick;
        public final double minDegreesPerTick;
        public final double acceleration;
        public final double jitter;
        public final double unusedJitterFrequency;
        public final double overshootChance;
        public final double maxOvershoot;

        public Config(boolean enabled, double maxDegreesPerTick, double minDegreesPerTick, double acceleration, double jitter, double unusedJitterFrequency, double overshootChance, double maxOvershoot) {
            this.enabled = enabled;
            this.maxDegreesPerTick = Math.max(0.0D, maxDegreesPerTick);
            this.minDegreesPerTick = Math.max(0.0D, minDegreesPerTick);
            this.acceleration = Math.max(0.0D, acceleration);
            this.jitter = Math.max(0.0D, jitter);
            this.unusedJitterFrequency = unusedJitterFrequency;
            this.overshootChance = overshootChance;
            this.maxOvershoot = Math.max(0.0D, maxOvershoot);
        }
    }
}
