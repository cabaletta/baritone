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

public enum PathEvent {
    CALC_STARTED,
    CALC_FINISHED_NOW_EXECUTING,
    CALC_FAILED,
    NEXT_SEGMENT_CALC_STARTED,
    NEXT_SEGMENT_CALC_FINISHED,
    CONTINUING_ONTO_PLANNED_NEXT,
    SPLICING_ONTO_NEXT_EARLY,
    AT_GOAL,
    PATH_FINISHED_NEXT_STILL_CALCULATING,
    NEXT_CALC_FAILED,
    DISCARD_NEXT,
    CANCELED;
}
