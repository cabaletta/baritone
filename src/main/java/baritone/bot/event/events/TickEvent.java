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

package baritone.bot.event.events;

import baritone.bot.event.events.type.EventState;
import javafx.event.EventType;

public final class TickEvent {

    private final EventState state;
    private final Type type;

    public TickEvent(EventState state, Type type) {
        this.state = state;
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public EventState getState() {
        return state;
    }


    public enum Type {
        /**
         * When guarantees can be made about
         * the game state and in-game variables.
         */
        IN,
        /**
         * No guarantees can be made about the game state.
         * This probably means we are at the main menu.
         */
        OUT,
    }
}
