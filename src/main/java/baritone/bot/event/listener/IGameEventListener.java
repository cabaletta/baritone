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
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;

/**
 * @author Brady
 * @since 7/31/2018 11:05 PM
 */
public interface IGameEventListener {

    /**
     * Run once per game tick before screen input is handled.
     *
     * @see Minecraft#runTick()
     */
    void onTick(TickEvent event);

    /**
     * Run once per game tick from before the player rotation is sent to the server.
     * @see EntityPlayerSP#onUpdate()
     */
    void onPlayerUpdate();

    /**
     * Run once per game tick from before keybinds are processed.
     *
     * @see Minecraft#processKeyBinds()
     */
    void onProcessKeyBinds();

    /**
     * Runs whenever the client player sends a message to the server.
     *
     * @see EntityPlayerSP#sendChatMessage(String)
     */
    void onSendChatMessage(ChatEvent event);

    /**
     * Runs before and after whenever a chunk is either loaded, unloaded, or populated.
     *
     * @see WorldClient#doPreChunk(int, int, boolean)
     */
    void onChunkEvent(ChunkEvent event);

    /**
     * Runs once per world render pass. Two passes are made when {@link GameSettings#anaglyph} is on.
     * <p>
     * <b>Note:</b> {@link GameSettings#anaglyph} has been removed in Minecraft 1.13
     *
     * @see EntityRenderer#renderWorldPass(int, float, long)
     */
    void onRenderPass(RenderEvent event);

    /**
     * Runs before and after whenever a new world is loaded
     *
     * @see Minecraft#loadWorld(WorldClient, String)
     */
    void onWorldEvent(WorldEvent event);

    /**
     * Runs before a outbound packet is sent
     *
     * @see NetworkManager#dispatchPacket(Packet, GenericFutureListener[])
     */
    void onSendPacket(PacketEvent event);

    /**
     * Runs before an inbound packet is processed
     *
     * @see NetworkManager#dispatchPacket(Packet, GenericFutureListener[])
     */
    void onReceivePacket(PacketEvent event);
}
