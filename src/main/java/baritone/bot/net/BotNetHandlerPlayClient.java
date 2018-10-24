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

package baritone.bot.net;

import baritone.bot.IBaritoneUser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.CPacketResourcePackStatus;
import net.minecraft.network.play.server.*;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;

// Notes:
// - All methods have been given a checkThreadAndEnqueue call, before implementing the handler, verify that it is in the actual NetHandlerPlayClient method

/**
 * @author Brady
 * @since 10/22/2018
 */
public class BotNetHandlerPlayClient implements INetHandlerPlayClient {

    private final NetworkManager networkManager;

    /**
     * This is the {@link Minecraft} game instance, however, to prevent unwanted references
     * to the game instance fields, we refer to it as a {@link IThreadListener}
     */
    private final IThreadListener client;

    /**
     * The bot of this connection
     */
    private final IBaritoneUser user;

    public BotNetHandlerPlayClient(NetworkManager networkManager, IThreadListener client, IBaritoneUser user) {
        this.networkManager = networkManager;
        this.client = client;
        this.user = user;
    }

    @Override
    public void handleSpawnObject(@Nonnull SPacketSpawnObject packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleSpawnExperienceOrb(@Nonnull SPacketSpawnExperienceOrb packetIn) { /* We will want to know this if we want Tenor to collect XP */ }

    @Override
    public void handleSpawnGlobalEntity(@Nonnull SPacketSpawnGlobalEntity packetIn) { /* Only lightning bolts, this may change in the future */ }

    @Override
    public void handleSpawnMob(@Nonnull SPacketSpawnMob packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleScoreboardObjective(@Nonnull SPacketScoreboardObjective packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleSpawnPainting(@Nonnull SPacketSpawnPainting packetIn) {}

    @Override
    public void handleSpawnPlayer(@Nonnull SPacketSpawnPlayer packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleAnimation(@Nonnull SPacketAnimation packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleStatistics(@Nonnull SPacketStatistics packetIn) { /* Lol global bot stats when?? */ }

    @Override
    public void handleRecipeBook(@Nonnull SPacketRecipeBook packetIn) {}

    @Override
    public void handleBlockBreakAnim(@Nonnull SPacketBlockBreakAnim packetIn) {}

    @Override
    public void handleSignEditorOpen(@Nonnull SPacketSignEditorOpen packetIn) {}

    @Override
    public void handleUpdateTileEntity(@Nonnull SPacketUpdateTileEntity packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleBlockAction(@Nonnull SPacketBlockAction packetIn) {}

    @Override
    public void handleBlockChange(@Nonnull SPacketBlockChange packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleChat(@Nonnull SPacketChat packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleTabComplete(@Nonnull SPacketTabComplete packetIn) {}

    @Override
    public void handleMultiBlockChange(@Nonnull SPacketMultiBlockChange packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleMaps(@Nonnull SPacketMaps packetIn) {}

    @Override
    public void handleConfirmTransaction(@Nonnull SPacketConfirmTransaction packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleCloseWindow(@Nonnull SPacketCloseWindow packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleWindowItems(@Nonnull SPacketWindowItems packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleOpenWindow(@Nonnull SPacketOpenWindow packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleWindowProperty(@Nonnull SPacketWindowProperty packetIn) {}

    @Override
    public void handleSetSlot(@Nonnull SPacketSetSlot packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleCustomPayload(@Nonnull SPacketCustomPayload packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleDisconnect(@Nonnull SPacketDisconnect packetIn) {
        this.networkManager.closeChannel(packetIn.getReason());
    }

    @Override
    public void handleUseBed(@Nonnull SPacketUseBed packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleEntityStatus(@Nonnull SPacketEntityStatus packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleEntityAttach(@Nonnull SPacketEntityAttach packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleSetPassengers(@Nonnull SPacketSetPassengers packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleExplosion(@Nonnull SPacketExplosion packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleChangeGameState(@Nonnull SPacketChangeGameState packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleKeepAlive(@Nonnull SPacketKeepAlive packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleChunkData(@Nonnull SPacketChunkData packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void processChunkUnload(@Nonnull SPacketUnloadChunk packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleEffect(@Nonnull SPacketEffect packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleJoinGame(@Nonnull SPacketJoinGame packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleEntityMovement(@Nonnull SPacketEntity packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handlePlayerPosLook(@Nonnull SPacketPlayerPosLook packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleParticles(@Nonnull SPacketParticles packetIn) {}

    @Override
    public void handlePlayerAbilities(@Nonnull SPacketPlayerAbilities packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handlePlayerListItem(@Nonnull SPacketPlayerListItem packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleDestroyEntities(@Nonnull SPacketDestroyEntities packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleRemoveEntityEffect(@Nonnull SPacketRemoveEntityEffect packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleRespawn(@Nonnull SPacketRespawn packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleEntityHeadLook(@Nonnull SPacketEntityHeadLook packetIn) {}

    @Override
    public void handleHeldItemChange(@Nonnull SPacketHeldItemChange packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleDisplayObjective(@Nonnull SPacketDisplayObjective packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleEntityMetadata(@Nonnull SPacketEntityMetadata packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleEntityVelocity(@Nonnull SPacketEntityVelocity packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleEntityEquipment(@Nonnull SPacketEntityEquipment packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleSetExperience(@Nonnull SPacketSetExperience packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleUpdateHealth(@Nonnull SPacketUpdateHealth packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleTeams(@Nonnull SPacketTeams packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleUpdateScore(@Nonnull SPacketUpdateScore packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleSpawnPosition(@Nonnull SPacketSpawnPosition packetIn) { /* We probably don't need to know this, the server handles everything related to spawn psoition? */ }

    @Override
    public void handleTimeUpdate(@Nonnull SPacketTimeUpdate packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleSoundEffect(@Nonnull SPacketSoundEffect packetIn) {}

    @Override
    public void handleCustomSound(@Nonnull SPacketCustomSound packetIn) {}

    @Override
    public void handleCollectItem(@Nonnull SPacketCollectItem packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleEntityTeleport(@Nonnull SPacketEntityTeleport packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleEntityProperties(@Nonnull SPacketEntityProperties packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleEntityEffect(@Nonnull SPacketEntityEffect packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleCombatEvent(@Nonnull SPacketCombatEvent packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleServerDifficulty(@Nonnull SPacketServerDifficulty packetIn) {}

    @Override
    public void handleCamera(SPacketCamera packetIn) {}

    @Override
    public void handleWorldBorder(@Nonnull SPacketWorldBorder packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleTitle(@Nonnull SPacketTitle packetIn) {}

    @Override
    public void handlePlayerListHeaderFooter(@Nonnull SPacketPlayerListHeaderFooter packetIn) {}

    @Override
    public void handleResourcePack(@Nonnull SPacketResourcePackSend packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
        /* Lie to the server and tell them we did it in response */
        this.networkManager.sendPacket(new CPacketResourcePackStatus(CPacketResourcePackStatus.Action.ACCEPTED));
        this.networkManager.sendPacket(new CPacketResourcePackStatus(CPacketResourcePackStatus.Action.SUCCESSFULLY_LOADED));
    }

    @Override
    public void handleUpdateBossInfo(@Nonnull SPacketUpdateBossInfo packetIn) {}

    @Override
    public void handleCooldown(@Nonnull SPacketCooldown packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleMoveVehicle(@Nonnull SPacketMoveVehicle packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleAdvancementInfo(@Nonnull SPacketAdvancementInfo packetIn) {}

    @Override
    public void handleSelectAdvancementsTab(@Nonnull SPacketSelectAdvancementsTab packetIn) { /* Lol global bot achievements when? */ }

    @Override
    public void func_194307_a(@Nonnull SPacketPlaceGhostRecipe p_194307_1_) {}

    @Override
    public void onDisconnect(@Nonnull ITextComponent reason) {
        /* Unload the world and notify the bot manager that we are no longer connected */
    }
}
