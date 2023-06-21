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
public interface IAimProcessor {

    /**
     * Returns the actual rotation that will be used when the desired rotation is requested. This is not guaranteed to
     * return the same value for a given input.
     *
     * @param desired The desired rotation to set
     * @return The actual rotation
     */
    Rotation nextRotation(Rotation desired);

    /**
     * Returns a copy of this {@link IAimProcessor} which has its own internal state and updates on each call to
     * {@link #nextRotation(Rotation)}.
     *
     * @param ticksAdvanced The number of ticks to advance ahead of time
     * @return The forked processor
     */
    IAimProcessor fork(int ticksAdvanced);
}
