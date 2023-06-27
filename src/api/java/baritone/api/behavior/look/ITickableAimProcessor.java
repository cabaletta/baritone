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

package baritone.api.behavior.look;

import baritone.api.utils.Rotation;

/**
 * @author Brady
 */
public interface ITickableAimProcessor extends IAimProcessor {

    /**
     * Advances the internal state of this aim processor by a single tick.
     */
    void tick();

    /**
     * Calls {@link #tick()} the specified number of times.
     *
     * @param ticks The number of calls
     */
    void advance(int ticks);

    /**
     * Returns the actual rotation as provided by {@link #peekRotation(Rotation)}, and then automatically advances the
     * internal state by one {@link #tick() tick}.
     *
     * @param rotation The desired rotation to set
     * @return The actual rotation
     */
    Rotation nextRotation(Rotation rotation);
}
