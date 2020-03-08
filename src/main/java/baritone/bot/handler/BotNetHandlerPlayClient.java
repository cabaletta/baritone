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
import baritone.bot.spec.BotMinecraft;
import baritone.bot.spec.BotPlayerController;
import baritone.bot.spec.BotWorld;
import baritone.bot.spec.EntityBot;
import baritone.utils.accessor.INetHandlerPlayClient;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.client.multiplayer.ClientAdvancementManager;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.util.RecipeBookClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.Explosion;

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
     * The bot's minecraft game instance. {@link BaritoneUser#getMinecraft()}
     */
    private final BotMinecraft client;

    /**
     * The bot of this connection
     */
    private final BaritoneUser user;

    /**
     * The bot entity
     */
    private EntityBot player;

    /**
     * The current world.
     */
    private BotWorld world;

    /**
     * The current player controller
     */
    private BotPlayerController playerController;

    public BotNetHandlerPlayClient(NetworkManager networkManager, BaritoneUser user, BotMinecraft client, GameProfile profile) {
        // noinspection ConstantConditions
        super(client, null, networkManager, profile);
        this.networkManager = networkManager;
        this.client = client;
        this.user = user;

        // Notify the user that we're ingame
        this.user.onLoginSuccess(profile, this);
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
    public void handleScoreboardObjective(@Nonnull SPacketScoreboardObjective packetIn) {}

    @Override
    public void handleSpawnPainting(@Nonnull SPacketSpawnPainting packetIn) {}

    @Override
    public void handleSpawnPlayer(@Nonnull SPacketSpawnPlayer packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleAnimation(@Nonnull SPacketAnimation packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);

        Entity entity = this.world.getEntityByID(packetIn.getEntityID());
        if (entity != null) {
            switch (packetIn.getAnimationType()) {
                case 0: {
                    ((EntityLivingBase) entity).swingArm(EnumHand.MAIN_HAND);
                    break;
                }
                case 1: {
                    entity.performHurtAnimation();
                    break;
                }
                case 2: {
                    ((EntityPlayer) entity).wakeUpPlayer(false, false, false);
                    break;
                }
                case 3: {
                    ((EntityLivingBase) entity).swingArm(EnumHand.OFF_HAND);
                    break;
                }
            }
        }
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
        super.handleBlockChange(packetIn);
    }

    @Override
    public void handleChat(@Nonnull SPacketChat packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

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
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
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
    public void handleOpenWindow(@Nonnull SPacketOpenWindow packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleWindowProperty(@Nonnull SPacketWindowProperty packetIn) {}

    @Override
    public void handleSetSlot(@Nonnull SPacketSetSlot packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);

        ItemStack stack = packetIn.getStack();
        int slot = packetIn.getSlot();

        switch (packetIn.getWindowId()) {
            case -1: {
                this.player.inventory.setItemStack(stack);
                break;
            }
            case -2: {
                this.player.inventory.setInventorySlotContents(slot, stack);
                break;
            }
            default: {
                if (packetIn.getWindowId() == 0 && packetIn.getSlot() >= 36 && slot < 45) {
                    this.player.inventoryContainer.putStackInSlot(slot, stack);
                } else if (packetIn.getWindowId() == this.player.openContainer.windowId && packetIn.getWindowId() != 0) {
                    this.player.openContainer.putStackInSlot(slot, stack);
                }
            }
        }
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

        // noinspection ConstantConditions
        new Explosion(this.world, null, packetIn.getX(), packetIn.getY(), packetIn.getZ(), packetIn.getStrength(), packetIn.getAffectedBlockPositions()).doExplosionB(true);
        this.player.motionX += packetIn.getMotionX();
        this.player.motionY += packetIn.getMotionY();
        this.player.motionZ += packetIn.getMotionZ();
    }

    @Override
    public void handleChangeGameState(@Nonnull SPacketChangeGameState packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleKeepAlive(@Nonnull SPacketKeepAlive packetIn) {
        this.networkManager.sendPacket(new CPacketKeepAlive(packetIn.getId()));
    }

    @Override
    public void handleChunkData(@Nonnull SPacketChunkData packetIn) {
        super.handleChunkData(packetIn);
    }

    @Override
    public void processChunkUnload(@Nonnull SPacketUnloadChunk packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
        // TODO Unload chunks
    }

    @Override
    public void handleEffect(@Nonnull SPacketEffect packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
    }

    @Override
    public void handleJoinGame(@Nonnull SPacketJoinGame packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);

        this.playerController = new BotPlayerController(this.user, this);
        this.world = this.user.getManager().getWorldProvider().getWorld(packetIn.getDimension());
        ((INetHandlerPlayClient) (Object) this).setWorld(this.world);
        this.player = new EntityBot(this.user, this.client, this.world, this, new StatisticsManager(), new RecipeBookClient());
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
    }

    @Override
    public void handlePlayerPosLook(@Nonnull SPacketPlayerPosLook packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);

        EntityPlayer player = this.player;
        double d0 = packetIn.getX();
        double d1 = packetIn.getY();
        double d2 = packetIn.getZ();
        float f = packetIn.getYaw();
        float f1 = packetIn.getPitch();

        if (packetIn.getFlags().contains(SPacketPlayerPosLook.EnumFlags.X)) {
            d0 += player.posX;
        } else {
            player.motionX = 0.0D;
        }

        if (packetIn.getFlags().contains(SPacketPlayerPosLook.EnumFlags.Y)) {
            d1 += player.posY;
        } else {
            player.motionY = 0.0D;
        }

        if (packetIn.getFlags().contains(SPacketPlayerPosLook.EnumFlags.Z)) {
            d2 += player.posZ;
        } else {
            player.motionZ = 0.0D;
        }

        if (packetIn.getFlags().contains(SPacketPlayerPosLook.EnumFlags.X_ROT)) {
            f1 += player.rotationPitch;
        }

        if (packetIn.getFlags().contains(SPacketPlayerPosLook.EnumFlags.Y_ROT)) {
            f += player.rotationYaw;
        }

        player.setPositionAndRotation(d0, d1, d2, f, f1);
        this.networkManager.sendPacket(new CPacketConfirmTeleport(packetIn.getTeleportId()));
        this.networkManager.sendPacket(new CPacketPlayer.PositionRotation(player.posX, player.getEntityBoundingBox().minY, player.posZ, player.rotationYaw, player.rotationPitch, false));

        this.player.prevPosX = this.player.posX;
        this.player.prevPosY = this.player.posY;
        this.player.prevPosZ = this.player.posZ;
    }

    @Override
    public void handleParticles(@Nonnull SPacketParticles packetIn) {}

    @Override
    public void handlePlayerAbilities(@Nonnull SPacketPlayerAbilities packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);

        PlayerCapabilities c = this.player.capabilities;
        c.disableDamage = packetIn.isInvulnerable();
        c.isFlying = packetIn.isFlying();
        c.allowFlying = packetIn.isAllowFlying();
        c.isCreativeMode = packetIn.isCreativeMode();
        c.setFlySpeed(packetIn.getFlySpeed());
        c.setPlayerWalkSpeed(packetIn.getWalkSpeed());
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
            this.world.removeEntity(this.player);
            this.world = this.user.getManager().getWorldProvider().getWorld(packetIn.getDimensionID());
            ((INetHandlerPlayClient) (Object) this).setWorld(this.world);
        }

        EntityBot prev = this.player;

        this.player = new EntityBot(this.user, this.client, this.world, this, prev.getStatFileWriter(), prev.getRecipeBook());
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
    public void handleAdvancementInfo(@Nonnull SPacketAdvancementInfo packetIn) {}

    @Override
    public void handleSelectAdvancementsTab(@Nonnull SPacketSelectAdvancementsTab packetIn) { /* Lol global bot achievements when? */ }

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

    public EntityBot player() {
        return player;
    }

    public BotWorld world() {
        return world;
    }
}
