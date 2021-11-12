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

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * @author Brady
 * @since 8/21/2018
 */
public final class RotationMoveEvent {

    /**
     * The type of event
     */
    private final Type type;

    /**
     * The yaw rotation
     */
    private float yaw;

    public RotationMoveEvent(Type type, float yaw) {
        this.type = type;
        this.yaw = yaw;
    }

    /**
     * Set the yaw movement rotation
     *
     * @param yaw Yaw rotation
     */
    public final void setYaw(float yaw) {
        this.yaw = yaw;
    }

    /**
     * @return The yaw rotation
     */
    public final float getYaw() {
        return this.yaw;
    }

    /**
     * @return The type of the event
     */
    public final Type getType() {
        return this.type;
    }

    public enum Type {

        /**
         * Called when the player's motion is updated.
         *
         * @see Entity#moveRelative(float, Vec3)
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
