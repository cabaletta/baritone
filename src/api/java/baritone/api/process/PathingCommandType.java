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

public enum PathingCommandType {
    SET_GOAL_AND_PATH, // self explanatory, if you do this one with a null goal it should continue

    REQUEST_PAUSE, // this one just pauses. it doesn't change the goal.

    CANCEL_AND_SET_GOAL, // cancel the current path, and set the goal (regardless of if it's null)

    REVALIDATE_GOAL_AND_PATH, // set the goal, revalidate if cancelOnGoalInvalidation is true, then path. if the goal is null, it will cancel (but only if that setting is true)
    FORCE_REVALIDATE_GOAL_AND_PATH // set the goal, revalidate current goal (cancel if no longer valid), cancel if the provided goal is null
}
