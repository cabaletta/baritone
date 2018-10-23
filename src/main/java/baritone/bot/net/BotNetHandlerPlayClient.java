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
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.*;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;

/**
 * @author Brady
 * @since 10/22/2018
 */
public class BotNetHandlerPlayClient implements INetHandlerPlayClient {

    private final IBaritoneUser user;

    public BotNetHandlerPlayClient(IBaritoneUser user) {
        this.user = user;
    }

    @Override
    public void handleSpawnObject(@Nonnull SPacketSpawnObject packetIn) {
        
    }

    @Override
    public void handleSpawnExperienceOrb(@Nonnull SPacketSpawnExperienceOrb packetIn) {

    }

    @Override
    public void handleSpawnGlobalEntity(@Nonnull SPacketSpawnGlobalEntity packetIn) {

    }

    @Override
    public void handleSpawnMob(@Nonnull SPacketSpawnMob packetIn) {

    }

    @Override
    public void handleScoreboardObjective(@Nonnull SPacketScoreboardObjective packetIn) {

    }

    @Override
    public void handleSpawnPainting(@Nonnull SPacketSpawnPainting packetIn) {

    }

    @Override
    public void handleSpawnPlayer(@Nonnull SPacketSpawnPlayer packetIn) {

    }

    @Override
    public void handleAnimation(@Nonnull SPacketAnimation packetIn) {

    }

    @Override
    public void handleStatistics(@Nonnull SPacketStatistics packetIn) {

    }

    @Override
    public void handleRecipeBook(@Nonnull SPacketRecipeBook packetIn) {

    }

    @Override
    public void handleBlockBreakAnim(@Nonnull SPacketBlockBreakAnim packetIn) {

    }

    @Override
    public void handleSignEditorOpen(@Nonnull SPacketSignEditorOpen packetIn) {

    }

    @Override
    public void handleUpdateTileEntity(@Nonnull SPacketUpdateTileEntity packetIn) {

    }

    @Override
    public void handleBlockAction(@Nonnull SPacketBlockAction packetIn) {

    }

    @Override
    public void handleBlockChange(@Nonnull SPacketBlockChange packetIn) {

    }

    @Override
    public void handleChat(@Nonnull SPacketChat packetIn) {

    }

    @Override
    public void handleTabComplete(@Nonnull SPacketTabComplete packetIn) {

    }

    @Override
    public void handleMultiBlockChange(@Nonnull SPacketMultiBlockChange packetIn) {

    }

    @Override
    public void handleMaps(@Nonnull SPacketMaps packetIn) {

    }

    @Override
    public void handleConfirmTransaction(@Nonnull SPacketConfirmTransaction packetIn) {

    }

    @Override
    public void handleCloseWindow(@Nonnull SPacketCloseWindow packetIn) {

    }

    @Override
    public void handleWindowItems(@Nonnull SPacketWindowItems packetIn) {

    }

    @Override
    public void handleOpenWindow(@Nonnull SPacketOpenWindow packetIn) {

    }

    @Override
    public void handleWindowProperty(@Nonnull SPacketWindowProperty packetIn) {

    }

    @Override
    public void handleSetSlot(@Nonnull SPacketSetSlot packetIn) {

    }

    @Override
    public void handleCustomPayload(@Nonnull SPacketCustomPayload packetIn) {

    }

    @Override
    public void handleDisconnect(@Nonnull SPacketDisconnect packetIn) {

    }

    @Override
    public void handleUseBed(@Nonnull SPacketUseBed packetIn) {

    }

    @Override
    public void handleEntityStatus(@Nonnull SPacketEntityStatus packetIn) {

    }

    @Override
    public void handleEntityAttach(@Nonnull SPacketEntityAttach packetIn) {

    }

    @Override
    public void handleSetPassengers(@Nonnull SPacketSetPassengers packetIn) {

    }

    @Override
    public void handleExplosion(@Nonnull SPacketExplosion packetIn) {

    }

    @Override
    public void handleChangeGameState(@Nonnull SPacketChangeGameState packetIn) {

    }

    @Override
    public void handleKeepAlive(@Nonnull SPacketKeepAlive packetIn) {

    }

    @Override
    public void handleChunkData(@Nonnull SPacketChunkData packetIn) {

    }

    @Override
    public void processChunkUnload(@Nonnull SPacketUnloadChunk packetIn) {

    }

    @Override
    public void handleEffect(@Nonnull SPacketEffect packetIn) {

    }

    @Override
    public void handleJoinGame(@Nonnull SPacketJoinGame packetIn) {

    }

    @Override
    public void handleEntityMovement(@Nonnull SPacketEntity packetIn) {

    }

    @Override
    public void handlePlayerPosLook(@Nonnull SPacketPlayerPosLook packetIn) {

    }

    @Override
    public void handleParticles(@Nonnull SPacketParticles packetIn) {

    }

    @Override
    public void handlePlayerAbilities(@Nonnull SPacketPlayerAbilities packetIn) {

    }

    @Override
    public void handlePlayerListItem(@Nonnull SPacketPlayerListItem packetIn) {

    }

    @Override
    public void handleDestroyEntities(@Nonnull SPacketDestroyEntities packetIn) {

    }

    @Override
    public void handleRemoveEntityEffect(@Nonnull SPacketRemoveEntityEffect packetIn) {

    }

    @Override
    public void handleRespawn(@Nonnull SPacketRespawn packetIn) {

    }

    @Override
    public void handleEntityHeadLook(@Nonnull SPacketEntityHeadLook packetIn) {

    }

    @Override
    public void handleHeldItemChange(@Nonnull SPacketHeldItemChange packetIn) {

    }

    @Override
    public void handleDisplayObjective(@Nonnull SPacketDisplayObjective packetIn) {

    }

    @Override
    public void handleEntityMetadata(@Nonnull SPacketEntityMetadata packetIn) {

    }

    @Override
    public void handleEntityVelocity(@Nonnull SPacketEntityVelocity packetIn) {

    }

    @Override
    public void handleEntityEquipment(@Nonnull SPacketEntityEquipment packetIn) {

    }

    @Override
    public void handleSetExperience(@Nonnull SPacketSetExperience packetIn) {

    }

    @Override
    public void handleUpdateHealth(@Nonnull SPacketUpdateHealth packetIn) {

    }

    @Override
    public void handleTeams(@Nonnull SPacketTeams packetIn) {

    }

    @Override
    public void handleUpdateScore(@Nonnull SPacketUpdateScore packetIn) {

    }

    @Override
    public void handleSpawnPosition(@Nonnull SPacketSpawnPosition packetIn) {

    }

    @Override
    public void handleTimeUpdate(@Nonnull SPacketTimeUpdate packetIn) {

    }

    @Override
    public void handleSoundEffect(@Nonnull SPacketSoundEffect packetIn) {

    }

    @Override
    public void handleCustomSound(@Nonnull SPacketCustomSound packetIn) {

    }

    @Override
    public void handleCollectItem(@Nonnull SPacketCollectItem packetIn) {

    }

    @Override
    public void handleEntityTeleport(@Nonnull SPacketEntityTeleport packetIn) {

    }

    @Override
    public void handleEntityProperties(@Nonnull SPacketEntityProperties packetIn) {

    }

    @Override
    public void handleEntityEffect(@Nonnull SPacketEntityEffect packetIn) {

    }

    @Override
    public void handleCombatEvent(@Nonnull SPacketCombatEvent packetIn) {

    }

    @Override
    public void handleServerDifficulty(@Nonnull SPacketServerDifficulty packetIn) {

    }

    @Override
    public void handleCamera(@Nonnull SPacketCamera packetIn) {

    }

    @Override
    public void handleWorldBorder(@Nonnull SPacketWorldBorder packetIn) {

    }

    @Override
    public void handleTitle(@Nonnull SPacketTitle packetIn) {

    }

    @Override
    public void handlePlayerListHeaderFooter(@Nonnull SPacketPlayerListHeaderFooter packetIn) {

    }

    @Override
    public void handleResourcePack(@Nonnull SPacketResourcePackSend packetIn) {

    }

    @Override
    public void handleUpdateBossInfo(@Nonnull SPacketUpdateBossInfo packetIn) {

    }

    @Override
    public void handleCooldown(@Nonnull SPacketCooldown packetIn) {

    }

    @Override
    public void handleMoveVehicle(@Nonnull SPacketMoveVehicle packetIn) {

    }

    @Override
    public void handleAdvancementInfo(@Nonnull SPacketAdvancementInfo packetIn) {

    }

    @Override
    public void handleSelectAdvancementsTab(@Nonnull SPacketSelectAdvancementsTab packetIn) {

    }

    @Override
    public void func_194307_a(SPacketPlaceGhostRecipe p_194307_1_) {

    }

    @Override
    public void onDisconnect(ITextComponent reason) {

    }
}
