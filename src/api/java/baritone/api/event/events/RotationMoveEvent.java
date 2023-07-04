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

package baritone.api.event.events;

import baritone.api.utils.Rotation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;

/**
 * @author Brady
 * @since 8/21/2018
 */
public final class RotationMoveEvent {

    /**
     * The type of event
     */
    private final Type type;

    private final Rotation original;

    /**
     * The yaw rotation
     */
    private float yaw;

    /**
     * The pitch rotation
     */
    private float pitch;

    public RotationMoveEvent(Type type, float yaw, float pitch) {
        this.type = type;
        this.original = new Rotation(yaw, pitch);
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Rotation getOriginal() {
        return this.original;
    }

    /**
     * Set the yaw movement rotation
     *
     * @param yaw Yaw rotation
     */
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    /**
     * @return The yaw rotation
     */
    public float getYaw() {
        return this.yaw;
    }

    /**
     * Set the pitch movement rotation
     *
     * @param pitch Pitch rotation
     */
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    /**
     * @return The pitch rotation
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * @return The type of the event
     */
    public Type getType() {
        return this.type;
    }

    public enum Type {

        /**
         * Called when the player's motion is updated.
         *
         * @see Entity#moveRelative(float, Vector3d)
         */
        MOTION_UPDATE,

        /**
         * Called when the player jumps.
         *
         * @see LivingEntity
         */
        JUMP
    }
}
