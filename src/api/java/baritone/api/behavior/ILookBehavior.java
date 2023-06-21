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

package baritone.api.behavior;

import baritone.api.Settings;
import baritone.api.behavior.look.IAimProcessor;
import baritone.api.utils.Rotation;

/**
 * @author Brady
 * @since 9/23/2018
 */
public interface ILookBehavior extends IBehavior {

    /**
     * Updates the current {@link ILookBehavior} target to target the specified rotations on the next tick. If any sort
     * of block interaction is required, {@code blockInteract} should be {@code true}. It is not guaranteed that the
     * rotations set by the caller will be the exact rotations expressed by the client (This is due to settings like
     * {@link Settings#randomLooking}). If the rotations produced by this behavior are required, then the
     * {@link #getAimProcessor() aim processor} should be used.
     *
     * @param rotation      The target rotations
     * @param blockInteract Whether the target rotations are needed for a block interaction
     */
    void updateTarget(Rotation rotation, boolean blockInteract);

    /**
     * The aim processor instance for this {@link ILookBehavior}, which is responsible for applying additional, deterministic
     * transformations to the target rotation set by {@link #updateTarget}. Whenever {@link IAimProcessor#nextRotation(Rotation)}
     * is called on the instance returned by this method, the returned value always reflects what would happen in the
     * upcoming tick. In other words, it is a pure function, and no internal state changes. If simulation of the
     * rotation states beyond the next tick is required, then a {@link IAimProcessor#fork(int) fork} should be created.
     *
     * @return The aim processor
     * @see IAimProcessor#fork(int)
     */
    IAimProcessor getAimProcessor();
}
