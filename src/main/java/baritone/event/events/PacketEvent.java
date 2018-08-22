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

package baritone.event.events;

import baritone.event.events.type.EventState;
import net.minecraft.network.Packet;

/**
 * @author Brady
 * @since 8/6/2018 9:31 PM
 */
public final class PacketEvent {

    private final EventState state;

    private final Packet<?> packet;

    public PacketEvent(EventState state, Packet<?> packet) {
        this.state = state;
        this.packet = packet;
    }

    public final EventState getState() {
        return this.state;
    }

    public final Packet<?> getPacket() {
        return this.packet;
    }

    @SuppressWarnings("unchecked")
    public final <T extends Packet<?>> T cast() {
        return (T) this.packet;
    }
}
