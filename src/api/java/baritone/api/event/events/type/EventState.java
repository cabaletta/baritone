/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.api.event.events.type;

/**
 * @author Brady
 * @since 8/2/2018 12:34 AM
 */
public enum EventState {

    /**
     * Indicates that whatever movement the event is being
     * dispatched as a result of is about to occur.
     */
    PRE,

    /**
     * Indicates that whatever movement the event is being
     * dispatched as a result of has already occurred.
     */
    POST
}
