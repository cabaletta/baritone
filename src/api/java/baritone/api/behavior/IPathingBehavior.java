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

import baritone.api.pathing.calc.IPathFinder;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.path.IPathExecutor;

import java.util.Optional;

/**
 * @author Brady
 * @since 9/23/2018
 */
public interface IPathingBehavior extends IBehavior {

    /**
     * Returns the estimated remaining ticks in the current pathing
     * segment. Given that the return type is an optional, {@link Optional#empty()}
     * will be returned in the case that there is no current segment being pathed.
     *
     * @return The estimated remaining ticks in the current segment.
     */
    Optional<Double> ticksRemainingInSegment();

    /**
     * Sets the pathing goal.
     *
     * @param goal The pathing goal
     */
    void setGoal(Goal goal);

    /**
     * @return The current pathing goal
     */
    Goal getGoal();

    /**
     * Begins pathing. Calculation will start in a new thread, and once completed,
     * movement will commence. Returns whether or not the operation was successful.
     *
     * @return Whether or not the operation was successful
     */
    boolean path();

    /**
     * @return Whether or not a path is currently being executed.
     */
    boolean isPathing();

    /**
     * Cancels the pathing behavior or the current path calculation.
     */
    void cancel();

    /**
     * Returns the current path, from the current path executor, if there is one.
     *
     * @return The current path
     */
    default Optional<IPath> getPath() {
        return Optional.ofNullable(getCurrent()).map(IPathExecutor::getPath);
    }

    /**
     * @return The current path finder being executed
     */
    Optional<IPathFinder> getPathFinder();

    /**
     * @return The current path executor
     */
    IPathExecutor getCurrent();

    /**
     * Returns the next path executor, created when planning ahead.
     *
     * @return The next path executor
     */
    IPathExecutor getNext();
}
