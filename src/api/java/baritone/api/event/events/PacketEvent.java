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
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;

/**
 * @author Brady
 * @since 8/6/2018
 */
public final class PacketEvent {

    private final NetworkManager networkManager;

    private final EventState state;

    private final Packet<?> packet;

    public PacketEvent(NetworkManager networkManager, EventState state, Packet<?> packet) {
        this.networkManager = networkManager;
        this.state = state;
        this.packet = packet;
    }

    public final NetworkManager getNetworkManager() {
        return this.networkManager;
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
