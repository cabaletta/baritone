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
public final class RotationUtils {

    private RotationUtils() {}

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
        if (newYaw >= 180F) {
            newYaw -= 360F;
        }
        return newYaw;
    }
}
