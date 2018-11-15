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

package baritone.api.event.listener;

/**
 * A type of {@link IGameEventListener} that can have additional listeners
 * registered so that they receive the events that are dispatched to this
 * listener.
 *
 * @author Brady
 * @since 11/14/2018
 */
public interface IEventBus extends IGameEventListener {

    /**
     * Registers the specified {@link IGameEventListener} to this event bus
     *
     * @param listener The listener
     */
    void registerEventListener(IGameEventListener listener);
}
