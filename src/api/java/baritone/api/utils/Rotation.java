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
    private float yaw;

    /**
     * The pitch angle of this Rotation
     */
    private float pitch;

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
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
                RotationUtils.clampPitch(this.pitch)
        );
    }

    /**
     * @return A copy of this rotation with the yaw normalized
     */
    public Rotation normalize() {
        return new Rotation(
                RotationUtils.normalizeYaw(this.yaw),
                this.pitch
        );
    }

    /**
     * @return A copy of this rotation with the pitch clamped and the yaw normalized
     */
    public Rotation normalizeAndClamp() {
        return new Rotation(
                RotationUtils.normalizeYaw(this.yaw),
                RotationUtils.clampPitch(this.pitch)
        );
    }
}
