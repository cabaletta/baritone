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

public final class VisualRotationHelper {

    private VisualRotationHelper() {}

    public static Rotation interpolate(Rotation from, Rotation to, float partialTicks) {
        float partial = Math.max(0.0F, Math.min(1.0F, partialTicks));
        float yawDelta = Rotation.normalizeYaw(to.getYaw() - from.getYaw());
        float pitchDelta = to.getPitch() - from.getPitch();
        return new Rotation(
                from.getYaw() + yawDelta * partial,
                from.getPitch() + pitchDelta * partial
        ).normalizeAndClamp();
    }

    public static Rotation movementPosture(Rotation heading, double pitch) {
        return new Rotation(heading.getYaw(), Rotation.clampPitch((float) pitch));
    }
}
