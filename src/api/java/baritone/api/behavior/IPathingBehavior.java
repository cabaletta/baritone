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

import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.calc.IPathFinder;
import baritone.api.pathing.goals.Goal;
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
    default Optional<Double> ticksRemainingInSegment() {
        return ticksRemainingInSegment(true);
    }

    /**
     * Returns the estimated remaining ticks in the current pathing
     * segment. Given that the return type is an optional, {@link Optional#empty()}
     * will be returned in the case that there is no current segment being pathed.
     *
     * @param includeCurrentMovement whether or not to include the entirety of the cost of the currently executing movement in the total
     * @return The estimated remaining ticks in the current segment.
     */
    default Optional<Double> ticksRemainingInSegment(boolean includeCurrentMovement) {
        IPathExecutor current = getCurrent();
        if (current == null) {
            return Optional.empty();
        }
        int start = includeCurrentMovement ? current.getPosition() : current.getPosition() + 1;
        return Optional.of(current.getPath().ticksRemainingFrom(start));
    }

    /**
     * Returns the estimated remaining ticks to the current goal.
     * Given that the return type is an optional, {@link Optional#empty()}
     * will be returned in the case that there is no current goal.
     *
     * @return The estimated remaining ticks to the current goal.
     */
    Optional<Double> estimatedTicksToGoal();

    /**
     * @return The current pathing goal
     */
    Goal getGoal();

    /**
     * @return Whether or not a path is currently being executed. This will be false if there's currently a pause.
     * @see #hasPath()
     */
    boolean isPathing();

    /**
     * @return If there is a current path. Note that the path is not necessarily being executed, for example when there
     * is a pause in effect.
     * @see #isPathing()
     */
    default boolean hasPath() {
        return getCurrent() != null;
    }

    /**
     * Cancels the pathing behavior or the current path calculation, and all processes that could be controlling path.
     * <p>
     * Basically, "MAKE IT STOP".
     *
     * @return Whether or not the pathing behavior was canceled. All processes are guaranteed to be canceled, but the
     * PathingBehavior might be in the middle of an uncancelable action like a parkour jump
     */
    boolean cancelEverything();

    /**
     * PLEASE never call this
     * <p>
     * If cancelEverything was like "kill" this is "sudo kill -9". Or shutting off your computer.
     */
    void forceCancel();

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
    Optional<? extends IPathFinder> getInProgress();

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
