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

import baritone.api.event.events.type.EventState;
import baritone.api.event.events.type.ManagedPlayerEvent;
import net.minecraft.client.entity.EntityPlayerSP;

/**
 * @author Brady
 * @since 8/21/2018
 */
public final class PlayerUpdateEvent extends ManagedPlayerEvent {

    /**
     * The state of the event
     */
    private final EventState state;

    public PlayerUpdateEvent(EntityPlayerSP player, EventState state) {
        super(player);
        this.state = state;
    }

    /**
     * @return The state of the event
     */
    public final EventState getState() {
        return this.state;
    }
}
