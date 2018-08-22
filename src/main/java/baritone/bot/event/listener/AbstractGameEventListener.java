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

package baritone.bot.event.listener;

import baritone.bot.event.events.*;

/**
 * An implementation of {@link IGameEventListener} that has all methods
 * overridden with empty bodies, allowing inheritors of this class to choose
 * which events they would like to listen in on.
 *
 * @see IGameEventListener
 *
 * @author Brady
 * @since 8/1/2018 6:29 PM
 */
public interface AbstractGameEventListener extends IGameEventListener {

    @Override
    default void onTick(TickEvent event) {}

    @Override
    default void onPlayerUpdate(PlayerUpdateEvent event) {}

    @Override
    default void onProcessKeyBinds() {}

    @Override
    default void onSendChatMessage(ChatEvent event) {}

    @Override
    default void onChunkEvent(ChunkEvent event) {}

    @Override
    default void onRenderPass(RenderEvent event) {}

    @Override
    default void onWorldEvent(WorldEvent event) {}

    @Override
    default void onSendPacket(PacketEvent event) {}

    @Override
    default void onReceivePacket(PacketEvent event) {}

    @Override
    default void onQueryItemSlotForBlocks(ItemSlotEvent event) {}

    @Override
    default void onPlayerRelativeMove(RelativeMoveEvent event) {}
}
