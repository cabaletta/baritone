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
 * Wrapper around a {@link baritone.api.process.IBaritoneProcess} used exclusively for pausing the Baritone.
 *
 * @author Crosby
 */
public interface IExecutionControl {
    /**
     * Tells Baritone to temporarily stop whatever it's doing until you resume.
     */
    void pause();

    /**
     * Resumes Baritone, resuming what it was doing when it was paused.
     */
    void resume();

    /**
     * Whether this instance of {@link IExecutionControl} is currently blocking Baritone from executing lower priority processes.
     *
     * @return whether Baritone is currently paused by this {@link IExecutionControl}.
     */
    boolean paused();

    /**
     * Cancels whatever Baritone is currently doing.
     */
    void stop();
}
