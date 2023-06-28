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

package baritone.bot.handler;

import baritone.api.utils.Helper;
import baritone.bot.BaritoneUser;
import baritone.bot.impl.BotPlayer;
import baritone.bot.impl.BotMinecraft;
import baritone.bot.impl.BotWorld;
import baritone.utils.accessor.INetHandlerPlayClient;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.client.multiplayer.ClientAdvancementManager;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.util.RecipeBookClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.client.CPacketClientStatus;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.network.play.client.CPacketResourcePackStatus;
import net.minecraft.network.play.server.*;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;

// Notes
// - Make some sort of system that prevents repetition of entity info updating
//   - For some packets, such as ones that modify position, we can check if the existing server state matches the packet proposed state
//   - For other things, we'll actually need the system

/**
 * @author Brady
 * @since 10/22/2018
 */
public final class BotNetHandlerPlayClient extends NetHandlerPlayClient {

    /**
     * The {@link NetworkManager} that is managing the connection with the server.
     */
    private final NetworkManager networkManager;

    /**
     * The bot's minecraft game instance
     */
    private final BotMinecraft client;

    /**
     * The bot of this connection
     */
    private final BaritoneUser user;

    /**
     * The bot player
     */
    private BotPlayer player;

    /**
     * The current world.
     */
    private BotWorld world;

    /**
     * The current player controller
     */
    private PlayerControllerMP playerController;

    BotNetHandlerPlayClient(NetworkManager networkManager, BaritoneUser user, BotMinecraft client, GameProfile profile) {
        // noinspection ConstantConditions
        super(client, null, networkManager, profile);
        this.networkManager = networkManager;
        this.client = client;
        this.user = user;

        // Notify the user that we're ingame
        this.user.onLoginSuccess(profile);
    }

    @Override
    public void handleSpawnObject(@Nonnull SPacketSpawnObject packetIn) {
        super.handleSpawnObject(packetIn);
    }

    @Override
    public void handleSpawnExperienceOrb(@Nonnull SPacketSpawnExperienceOrb packetIn) {
        super.handleSpawnExperienceOrb(packetIn);
    }

    @Override
    public void handleSpawnGlobalEntity(@Nonnull SPacketSpawnGlobalEntity packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);

        if (this.world != null) {
            if (this.world.weatherEffects.stream().noneMatch(entity -> entity.getEntityId() == packetIn.getEntityId())) {
                super.handleSpawnGlobalEntity(packetIn);
            }
        }
    }

    @Override
    public void handleSpawnMob(@Nonnull SPacketSpawnMob packetIn) {}

    @Override
    public void handleScoreboardObjective(@Nonnull SPacketScoreboardObjective packetIn) {}

    @Override
    public void handleSpawnPainting(@Nonnull SPacketSpawnPainting packetIn) {}

    @Override
    public void handleSpawnPlayer(@Nonnull SPacketSpawnPlayer packetIn) {}

    @Override
    public void handleAnimation(@Nonnull SPacketAnimation packetIn) {
        super.handleAnimation(packetIn);
    }

    @Override
    public void handleStatistics(@Nonnull SPacketStatistics packetIn) {
        super.handleStatistics(packetIn);
    }

    @Override
    public void handleRecipeBook(@Nonnull SPacketRecipeBook packetIn) {}

    @Override
    public void handleBlockBreakAnim(@Nonnull SPacketBlockBreakAnim packetIn) {
        super.handleBlockBreakAnim(packetIn);
    }

    @Override
    public void handleSignEditorOpen(@Nonnull SPacketSignEditorOpen packetIn) {}

    @Override
    public void handleUpdateTileEntity(@Nonnull SPacketUpdateTileEntity packetIn) {}

    @Override
    public void handleBlockAction(@Nonnull SPacketBlockAction packetIn) {}

    @Override
    public void handleBlockChange(@Nonnull SPacketBlockChange packetIn) {
        super.handleBlockChange(packetIn);
    }

    @Override
    public void handleChat(@Nonnull SPacketChat packetIn) {}

    @Override
    public void handleTabComplete(@Nonnull SPacketTabComplete packetIn) {}

    @Override
    public void handleMultiBlockChange(@Nonnull SPacketMultiBlockChange packetIn) {
        super.handleMultiBlockChange(packetIn);
    }

    @Override
    public void handleMaps(@Nonnull SPacketMaps packetIn) {}

    @Override
    public void handleConfirmTransaction(@Nonnull SPacketConfirmTransaction packetIn) {
        super.handleConfirmTransaction(packetIn);
    }

    @Override
    public void handleCloseWindow(@Nonnull SPacketCloseWindow packetIn) {
        super.handleCloseWindow(packetIn);
    }

    @Override
    public void handleWindowItems(@Nonnull SPacketWindowItems packetIn) {
        super.handleWindowItems(packetIn);
    }

    @Override
    public void handleOpenWindow(@Nonnull SPacketOpenWindow packetIn) {}

    @Override
    public void handleWindowProperty(@Nonnull SPacketWindowProperty packetIn) {
        super.handleWindowProperty(packetIn);
    }

    @Override
    public void handleSetSlot(@Nonnull SPacketSetSlot packetIn) {
        super.handleSetSlot(packetIn);
    }

    @Override
    public void handleCustomPayload(@Nonnull SPacketCustomPayload packetIn) {}

    @Override
    public void handleDisconnect(@Nonnull SPacketDisconnect packetIn) {
        super.handleDisconnect(packetIn);
    }

    @Override
    public void handleUseBed(@Nonnull SPacketUseBed packetIn) {}

    @Override
    public void handleEntityStatus(@Nonnull SPacketEntityStatus packetIn) {
        super.handleEntityStatus(packetIn);
    }

    @Override
    public void handleEntityAttach(@Nonnull SPacketEntityAttach packetIn) {
        super.handleEntityAttach(packetIn);
    }

    @Override
    public void handleSetPassengers(@Nonnull SPacketSetPassengers packetIn) {}

    @Override
    public void handleExplosion(@Nonnull SPacketExplosion packetIn) {
        super.handleExplosion(packetIn);
    }

    @Override
    public void handleChangeGameState(@Nonnull SPacketChangeGameState packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);

        if (packetIn.getGameState() == 4) {
            this.client.player.connection.sendPacket(new CPacketClientStatus(CPacketClientStatus.State.PERFORM_RESPAWN));
        } else if (packetIn.getGameState() != 5) {
            super.handleChangeGameState(packetIn);
        }
    }

    @Override
    public void handleKeepAlive(@Nonnull SPacketKeepAlive packetIn) {
        super.handleKeepAlive(packetIn);
    }

    @Override
    public void handleChunkData(@Nonnull SPacketChunkData packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);

        if (packetIn.isFullChunk()) {
            if (!this.world.handlePreChunk(this.player, packetIn.getChunkX(), packetIn.getChunkZ(), true)) {
                return;
            }
        }
        super.handleChunkData(packetIn);
    }

    @Override
    public void processChunkUnload(@Nonnull SPacketUnloadChunk packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);

        if (this.world.handlePreChunk(this.player, packetIn.getX(), packetIn.getZ(), false)) {
            super.processChunkUnload(packetIn);
        }
    }

    @Override
    public void handleEffect(@Nonnull SPacketEffect packetIn) {}

    @Override
    public void handleJoinGame(@Nonnull SPacketJoinGame packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);

        this.playerController = new PlayerControllerMP(this.user.getPlayerContext().minecraft(), this);
        this.world = this.user.getManager().getWorldProvider().getWorld(packetIn.getDimension());
        ((INetHandlerPlayClient) (Object) this).setWorld(this.world);
        this.player = new BotPlayer(this.user, this.client, this.world, this, new StatisticsManager(), new RecipeBookClient());
        this.user.onWorldLoad(this.world, this.player, this.playerController);
        this.player.preparePlayerToSpawn();
        this.player.setEntityId(packetIn.getPlayerId());
        this.player.dimension = packetIn.getDimension();
        this.world.addEntityToWorld(packetIn.getPlayerId(), this.player);
        this.playerController.setGameType(packetIn.getGameType());
        packetIn.getGameType().configurePlayerCapabilities(this.player.capabilities);

        this.client.gameSettings.sendSettingsToServer();
        this.networkManager.sendPacket(new CPacketCustomPayload("MC|Brand", new PacketBuffer(Unpooled.buffer()).writeString("vanilla")));

        Helper.HELPER.logDirect("Initialized Player and World");
    }

    @Override
    public void handleEntityMovement(@Nonnull SPacketEntity packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);

        Entity e = packetIn.getEntity(this.world);
        if (e instanceof BotPlayer && !e.equals(this.player)) {
            return;
        }

        super.handleEntityMovement(packetIn);
    }

    @Override
    public void handlePlayerPosLook(@Nonnull SPacketPlayerPosLook packetIn) {
        super.handlePlayerPosLook(packetIn);
    }

    @Override
    public void handleParticles(@Nonnull SPacketParticles packetIn) {}

    @Override
    public void handlePlayerAbilities(@Nonnull SPacketPlayerAbilities packetIn) {
        super.handlePlayerAbilities(packetIn);
    }

    @Override
    public void handlePlayerListItem(@Nonnull SPacketPlayerListItem packetIn) {
        super.handlePlayerListItem(packetIn);
    }

    @Override
    public void handleDestroyEntities(@Nonnull SPacketDestroyEntities packetIn) {
        super.handleDestroyEntities(packetIn);
    }

    @Override
    public void handleRemoveEntityEffect(@Nonnull SPacketRemoveEntityEffect packetIn) {
        super.handleRemoveEntityEffect(packetIn);
    }

    @Override
    public void handleRespawn(@Nonnull SPacketRespawn packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);

        if (packetIn.getDimensionID() != this.player.dimension) {
            this.world.handleWorldRemove(this.player);
            this.world = this.user.getManager().getWorldProvider().getWorld(packetIn.getDimensionID());
            ((INetHandlerPlayClient) (Object) this).setWorld(this.world);
        }

        BotPlayer prev = this.player;

        this.player = new BotPlayer(this.user, this.client, this.world, this, prev.getStatFileWriter(), prev.getRecipeBook());
        this.user.onWorldLoad(this.world, this.player, this.playerController);
        // noinspection ConstantConditions
        this.player.getDataManager().setEntryValues(prev.getDataManager().getAll());
        this.player.preparePlayerToSpawn();
        this.player.setEntityId(prev.getEntityId());
        this.player.dimension = packetIn.getDimensionID();
        this.player.setServerBrand(prev.getServerBrand());
        this.world.addEntityToWorld(prev.getEntityId(), this.player);
        this.playerController.setGameType(packetIn.getGameType());
    }

    @Override
    public void handleEntityHeadLook(@Nonnull SPacketEntityHeadLook packetIn) {}

    @Override
    public void handleHeldItemChange(@Nonnull SPacketHeldItemChange packetIn) {
        super.handleHeldItemChange(packetIn);
    }

    @Override
    public void handleDisplayObjective(@Nonnull SPacketDisplayObjective packetIn) {}

    @Override
    public void handleEntityMetadata(@Nonnull SPacketEntityMetadata packetIn) {
        super.handleEntityMetadata(packetIn);
    }

    @Override
    public void handleEntityVelocity(@Nonnull SPacketEntityVelocity packetIn) {
        super.handleEntityVelocity(packetIn);
    }

    @Override
    public void handleEntityEquipment(@Nonnull SPacketEntityEquipment packetIn) {
        super.handleEntityEquipment(packetIn);
    }

    @Override
    public void handleSetExperience(@Nonnull SPacketSetExperience packetIn) {
        super.handleSetExperience(packetIn);
    }

    @Override
    public void handleUpdateHealth(@Nonnull SPacketUpdateHealth packetIn) {
        super.handleUpdateHealth(packetIn);
    }

    @Override
    public void handleTeams(@Nonnull SPacketTeams packetIn) {}

    @Override
    public void handleUpdateScore(@Nonnull SPacketUpdateScore packetIn) {}

    @Override
    public void handleSpawnPosition(@Nonnull SPacketSpawnPosition packetIn) { /* We probably don't need to know this, the server handles everything related to spawn psoition? */ }

    @Override
    public void handleTimeUpdate(@Nonnull SPacketTimeUpdate packetIn) {
        super.handleTimeUpdate(packetIn);
    }

    @Override
    public void handleSoundEffect(@Nonnull SPacketSoundEffect packetIn) {}

    @Override
    public void handleCustomSound(@Nonnull SPacketCustomSound packetIn) {}

    @Override
    public void handleCollectItem(@Nonnull SPacketCollectItem packetIn) {
        super.handleCollectItem(packetIn);
    }

    @Override
    public void handleEntityTeleport(@Nonnull SPacketEntityTeleport packetIn) {
        super.handleEntityTeleport(packetIn);
    }

    @Override
    public void handleEntityProperties(@Nonnull SPacketEntityProperties packetIn) {
        super.handleEntityProperties(packetIn);
    }

    @Override
    public void handleEntityEffect(@Nonnull SPacketEntityEffect packetIn) {
        super.handleEntityEffect(packetIn);
    }

    @Override
    public void handleCombatEvent(@Nonnull SPacketCombatEvent packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);

        // We only care if we died
        if (packetIn.eventType == SPacketCombatEvent.Event.ENTITY_DIED) {
            if (packetIn.playerId == this.player.getEntityId()) {
                // Perform an instantaneous respawn
                this.networkManager.sendPacket(new CPacketClientStatus(CPacketClientStatus.State.PERFORM_RESPAWN));
                user.getBaritone().getGameEventHandler().onPlayerDeath();
            }
        }
    }

    @Override
    public void handleServerDifficulty(@Nonnull SPacketServerDifficulty packetIn) {}

    @Override
    public void handleCamera(SPacketCamera packetIn) {}

    @Override
    public void handleWorldBorder(@Nonnull SPacketWorldBorder packetIn) {
        super.handleWorldBorder(packetIn);
    }

    @Override
    public void handleTitle(@Nonnull SPacketTitle packetIn) {}

    @Override
    public void handlePlayerListHeaderFooter(@Nonnull SPacketPlayerListHeaderFooter packetIn) {}

    @Override
    public void handleResourcePack(@Nonnull SPacketResourcePackSend packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);

        // Lie to the server and tell them we accepted it in response
        this.networkManager.sendPacket(new CPacketResourcePackStatus(CPacketResourcePackStatus.Action.ACCEPTED));
        this.networkManager.sendPacket(new CPacketResourcePackStatus(CPacketResourcePackStatus.Action.SUCCESSFULLY_LOADED));
    }

    @Override
    public void handleUpdateBossInfo(@Nonnull SPacketUpdateBossInfo packetIn) {}

    @Override
    public void handleCooldown(@Nonnull SPacketCooldown packetIn) {
        super.handleCooldown(packetIn);
    }

    @Override
    public void handleMoveVehicle(@Nonnull SPacketMoveVehicle packetIn) {
        super.handleMoveVehicle(packetIn);
    }

    @Override
    public void handleAdvancementInfo(@Nonnull SPacketAdvancementInfo packetIn) {
        super.handleAdvancementInfo(packetIn);
    }

    @Override
    public void handleSelectAdvancementsTab(@Nonnull SPacketSelectAdvancementsTab packetIn) {
        super.handleSelectAdvancementsTab(packetIn);
    }

    @Override
    public void func_194307_a(@Nonnull SPacketPlaceGhostRecipe p_194307_1_) {}

    @Override
    public void onDisconnect(@Nonnull ITextComponent reason) {
        // TODO Maybe more world unloading
        this.world.removeEntity(this.player);
        this.user.getManager().disconnect(this.user, reason);
    }

    @Nonnull
    @Override
    public ClientAdvancementManager getAdvancementManager() {
        throw new UnsupportedOperationException("This method shouldn't have been called; That is unepic!");
    }

    public BotPlayer player() {
        return player;
    }

    public BotWorld world() {
        return world;
    }
}
