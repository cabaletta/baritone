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

import baritone.api.pathing.goals.Goal;

public interface ICustomGoalProcess extends IBaritoneProcess {

    /**
     * Sets the pathing goal
     *
     * @param goal The new goal
     */
    void setGoal(Goal goal);

    /**
     * Starts path calculation and execution.
     */
    void path();

    /**
     * @return The current goal
     */
    Goal getGoal();

    /**
     * @return The most recent set goal, which doesn't invalidate upon {@link #onLostControl()}
     */
    Goal mostRecentGoal();

    /**
     * Sets the goal and begins the path execution.
     *
     * @param goal The new goal
     */
    default void setGoalAndPath(Goal goal) {
        this.setGoal(goal);
        this.path();
    }
}
