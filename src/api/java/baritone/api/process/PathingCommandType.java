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

package baritone.api.process;

import baritone.api.Settings;

public enum PathingCommandType {

    /**
     * Set the goal and path.
     * <p>
     * If you use this alongside a {@code null} goal, it will continue along its current path and current goal.
     */
    SET_GOAL_AND_PATH,

    /**
     * Has no effect on the current goal or path, just requests a pause
     */
    REQUEST_PAUSE,

    /**
     * Set the goal (regardless of {@code null}), and request a cancel of the current path (when safe)
     */
    CANCEL_AND_SET_GOAL,

    /**
     * Set the goal and path.
     * <p>
     * If {@link Settings#cancelOnGoalInvalidation} is {@code true}, revalidate the
     * current goal, and cancel if it's no longer valid, or if the new goal is {@code null}.
     */
    REVALIDATE_GOAL_AND_PATH,

    /**
     * Set the goal and path.
     * <p>
     * Cancel the current path if the goals are not equal
     */
    FORCE_REVALIDATE_GOAL_AND_PATH,

    /**
     * Go and ask the next process what to do
     */
    DEFER,

    /**
     * Sets the goal and calculates a path, but pauses instead of immediately starting the path.
     */
    SET_GOAL_AND_PAUSE
}
