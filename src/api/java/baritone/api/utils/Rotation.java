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

package baritone.api.utils;

/**
 * @author Brady
 * @since 9/25/2018
 */
public class Rotation {

    /**
     * The yaw angle of this Rotation
     */
    private final float yaw;

    /**
     * The pitch angle of this Rotation
     */
    private final float pitch;

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        if (Float.isInfinite(yaw) || Float.isNaN(yaw) || Float.isInfinite(pitch) || Float.isNaN(pitch)) {
            throw new IllegalStateException(yaw + " " + pitch);
        }
    }

    /**
     * @return The yaw of this rotation
     */
    public float getYaw() {
        return this.yaw;
    }

    /**
     * @return The pitch of this rotation
     */
    public float getPitch() {
        return this.pitch;
    }

    /**
     * Adds the yaw/pitch of the specified rotations to this
     * rotation's yaw/pitch, and returns the result.
     *
     * @param other Another rotation
     * @return The result from adding the other rotation to this rotation
     */
    public Rotation add(Rotation other) {
        return new Rotation(
                this.yaw + other.yaw,
                this.pitch + other.pitch
        );
    }

    /**
     * Subtracts the yaw/pitch of the specified rotations from this
     * rotation's yaw/pitch, and returns the result.
     *
     * @param other Another rotation
     * @return The result from subtracting the other rotation from this rotation
     */
    public Rotation subtract(Rotation other) {
        return new Rotation(
                this.yaw - other.yaw,
                this.pitch - other.pitch
        );
    }

    /**
     * @return A copy of this rotation with the pitch clamped
     */
    public Rotation clamp() {
        return new Rotation(
                this.yaw,
                clampPitch(this.pitch)
        );
    }

    /**
     * @return A copy of this rotation with the yaw normalized
     */
    public Rotation normalize() {
        return new Rotation(
                normalizeYaw(this.yaw),
                this.pitch
        );
    }

    /**
     * @return A copy of this rotation with the pitch clamped and the yaw normalized
     */
    public Rotation normalizeAndClamp() {
        return new Rotation(
                normalizeYaw(this.yaw),
                clampPitch(this.pitch)
        );
    }

    public Rotation withPitch(float pitch) {
        return new Rotation(this.yaw, pitch);
    }

    /**
     * Is really close to
     *
     * @param other another rotation
     * @return are they really close
     */
    public boolean isReallyCloseTo(Rotation other) {
        return yawIsReallyClose(other) && Math.abs(this.pitch - other.pitch) < 0.01;
    }

    public boolean yawIsReallyClose(Rotation other) {
        float yawDiff = Math.abs(normalizeYaw(yaw) - normalizeYaw(other.yaw)); // you cant fool me
        return (yawDiff < 0.01 || yawDiff > 359.99);
    }

    /**
     * Clamps the specified pitch value between -90 and 90.
     *
     * @param pitch The input pitch
     * @return The clamped pitch
     */
    public static float clampPitch(float pitch) {
        return Math.max(-90, Math.min(90, pitch));
    }

    /**
     * Normalizes the specified yaw value between -180 and 180.
     *
     * @param yaw The input yaw
     * @return The normalized yaw
     */
    public static float normalizeYaw(float yaw) {
        float newYaw = yaw % 360F;
        if (newYaw < -180F) {
            newYaw += 360F;
        }
        if (newYaw > 180F) {
            newYaw -= 360F;
        }
        return newYaw;
    }

    @Override
    public String toString() {
        return "Yaw: " + yaw + ", Pitch: " + pitch;
    }
}
