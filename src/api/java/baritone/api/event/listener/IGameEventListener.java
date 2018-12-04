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

import baritone.api.event.events.*;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.util.text.ITextComponent;

/**
 * @author Brady
 * @since 7/31/2018
 */
public interface IGameEventListener {

    /**
     * Run once per game tick before screen input is handled.
     *
     * @see Minecraft#runTick()
     *
     * @param event The event
     */
    void onTick(TickEvent event);

    /**
     * Run once per game tick from before and after the player rotation is sent to the server.
     *
     * @see EntityPlayerSP#onUpdate()
     *
     * @param event The event
     */
    void onPlayerUpdate(PlayerUpdateEvent event);

    /**
     * Run once per game tick from before keybinds are processed.
     */
    void onProcessKeyBinds();

    /**
     * Runs whenever the client player sends a message to the server.
     *
     * @see EntityPlayerSP#sendChatMessage(String)
     *
     * @param event The event
     */
    void onSendChatMessage(ChatEvent event);

    /**
     * Runs before and after whenever a chunk is either loaded, unloaded, or populated.
     *
     * @see WorldClient#doPreChunk(int, int, boolean)
     *
     * @param event The event
     */
    void onChunkEvent(ChunkEvent event);

    /**
     * Runs once per world render pass. Two passes are made when {@link GameSettings#anaglyph} is on.
     * <p>
     * <b>Note:</b> {@link GameSettings#anaglyph} has been removed in Minecraft 1.13
     *
     * @param event The event
     */
    void onRenderPass(RenderEvent event);

    /**
     * Runs before and after whenever a new world is loaded
     *
     * @see Minecraft#loadWorld(WorldClient, String)
     *
     * @param event The event
     */
    void onWorldEvent(WorldEvent event);

    /**
     * Runs before a outbound packet is sent
     *
     * @see Packet
     * @see GenericFutureListener
     *
     * @param event The event
     */
    void onSendPacket(PacketEvent event);

    /**
     * Runs before an inbound packet is processed
     *
     * @see Packet
     * @see GenericFutureListener
     *
     * @param event The event
     */
    void onReceivePacket(PacketEvent event);

    /**
     * Run once per game tick from before and after the player's moveRelative method is called
     * and before and after the player jumps.
     *
     * @see Entity#moveRelative(float, float, float, float)
     *
     * @param event The event
     */
    void onPlayerRotationMove(RotationMoveEvent event);

    /**
     * Called when the local player interacts with a block, whether it is breaking or opening/placing.
     *
     * @param event The event
     */
    void onBlockInteract(BlockInteractEvent event);

    /**
     * Called when the local player dies, as indicated by the creation of the {@link GuiGameOver} screen.
     *
     * @see GuiGameOver
     */
    void onPlayerDeath();

    /**
     * When the pathfinder's state changes
     *
     * @param event The event
     */
    void onPathEvent(PathEvent event);
}
