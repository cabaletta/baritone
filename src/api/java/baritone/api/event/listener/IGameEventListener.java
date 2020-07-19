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
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.IPacket;
import net.minecraft.util.math.vector.Vector3d;

/**
 * @author Brady
 * @since 7/31/2018
 */
public interface IGameEventListener {

    /**
     * Run once per game tick before screen input is handled.
     *
     * @param event The event
     * @see Minecraft#runTick()
     */
    void onTick(TickEvent event);

    /**
     * Run once per game tick from before and after the player rotation is sent to the server.
     *
     * @param event The event
     * @see ClientPlayerEntity#tick()
     */
    void onPlayerUpdate(PlayerUpdateEvent event);

    /**
     * Runs whenever the client player sends a message to the server.
     *
     * @param event The event
     * @see ClientPlayerEntity#sendChatMessage(String)
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
     * Runs once per world render pass.
     *
     * @param event The event
     */
    void onRenderPass(RenderEvent event);

    /**
     * Runs before and after whenever a new world is loaded
     *
     * @param event The event
     * @see Minecraft#loadWorld(ClientWorld)
     */
    void onWorldEvent(WorldEvent event);

    /**
     * Runs before a outbound packet is sent
     *
     * @param event The event
     * @see IPacket
     */
    void onSendPacket(PacketEvent event);

    /**
     * Runs before an inbound packet is processed
     *
     * @param event The event
     * @see IPacket
     */
    void onReceivePacket(PacketEvent event);

    /**
     * Run once per game tick from before and after the player's moveRelative method is called
     * and before and after the player jumps.
     *
     * @param event The event
     * @see Entity#moveRelative(float, Vector3d)
     */
    void onPlayerRotationMove(RotationMoveEvent event);

    /**
     * Called whenever the sprint keybind state is checked in {@link ClientPlayerEntity#livingTick}
     *
     * @param event The event
     * @see ClientPlayerEntity#livingTick()
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
