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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * @author Brady
 * @since 7/31/2018
 */
public interface IGameEventListener {

    /**
     * Run once per game tick before screen input is handled.
     *
     * @param event The event
     * @see Minecraft#tick()
     */
    void onTick(TickEvent event);

    /**
     * Run once per game tick after the tick is completed
     *
     * @param event The event
     * @see Minecraft#runTick()
     */
    void onPostTick(TickEvent event);

    /**
     * Run once per game tick from before and after the player rotation is sent to the server.
     *
     * @param event The event
     * @see LocalPlayer#tick()
     */
    void onPlayerUpdate(PlayerUpdateEvent event);

    /**
     * Runs whenever the client player sends a message to the server.
     *
     * @param event The event
     * @see LocalPlayer#chat(String)
     */
    void onSendChatMessage(ChatEvent event);

    /**
     * Runs whenever the client player tries to tab complete in chat.
     *
     * @param event The event
     */
    void onPreTabComplete(TabCompleteEvent event);

    /**
     * Runs before and after whenever a chunk is either loaded, unloaded, or populated.
     *
     * @param event The event
     */
    void onChunkEvent(ChunkEvent event);

    /**
     * Runs after a single or multi block change packet is received and processed.
     *
     * @param event The event
     */
    void onBlockChange(BlockChangeEvent event);

    /**
     * Runs once per world render pass.
     *
     * @param event The event
     */
    void onRenderPass(RenderEvent event);

    /**
     * Runs before and after whenever a new world is loaded
     *
     * @param event The event
     * @see Minecraft#setLevel(ClientLevel)
     */
    void onWorldEvent(WorldEvent event);

    /**
     * Runs before a outbound packet is sent
     *
     * @param event The event
     * @see Packet
     */
    void onSendPacket(PacketEvent event);

    /**
     * Runs before an inbound packet is processed
     *
     * @param event The event
     * @see Packet
     */
    void onReceivePacket(PacketEvent event);

    /**
     * Run once per game tick from before and after the player's moveRelative method is called
     * and before and after the player jumps.
     *
     * @param event The event
     * @see Entity#moveRelative(float, Vec3)
     */
    void onPlayerRotationMove(RotationMoveEvent event);

    /**
     * Called whenever the sprint keybind state is checked in {@link LocalPlayer#aiStep}
     *
     * @param event The event
     * @see LocalPlayer#aiStep()
     */
    void onPlayerSprintState(SprintStateEvent event);

    /**
     * Called when the local player interacts with a block, whether it is breaking or opening/placing.
     *
     * @param event The event
     */
    void onBlockInteract(BlockInteractEvent event);

    /**
     * Called when the local player dies, as indicated by the creation of the {@link DeathScreen} screen.
     *
     * @see DeathScreen
     */
    void onPlayerDeath();

    /**
     * When the pathfinder's state changes
     *
     * @param event The event
     */
    void onPathEvent(PathEvent event);
}
