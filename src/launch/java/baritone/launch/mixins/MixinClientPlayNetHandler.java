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

package baritone.launch.mixins;

import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.event.events.ChunkEvent;
import baritone.api.event.events.type.EventState;
import baritone.cache.CachedChunk;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.play.server.*;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Brady
 * @since 8/3/2018
 */
@Mixin(ClientPacketListener.class)
public class MixinClientPlayNetHandler {

    // unused lol
    /*@Inject(
            method = "handleChunkData",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/multiplayer/ChunkProviderClient.func_212474_a(IILnet/minecraft/network/PacketBuffer;IZ)Lnet/minecraft/world/chunk/Chunk;"
            )
    )
    private void preRead(SPacketChunkData packetIn, CallbackInfo ci) {
        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            ClientPlayerEntity player = ibaritone.getPlayerContext().player();
            if (player != null && player.connection == (ClientPlayNetHandler) (Object) this) {
                ibaritone.getGameEventHandler().onChunkEvent(
                        new ChunkEvent(
                                EventState.PRE,
                                packetIn.isFullChunk() ? ChunkEvent.Type.POPULATE_FULL : ChunkEvent.Type.POPULATE_PARTIAL,
                                packetIn.getChunkX(),
                                packetIn.getChunkZ()
                        )
                );
            }
        }
    }*/

    @Inject(
            method = "handleChunkData",
            at = @At("RETURN")
    )
    private void postHandleChunkData(ClientboundLevelChunkPacket packetIn, CallbackInfo ci) {
        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            LocalPlayer player = ibaritone.getPlayerContext().player();
            if (player != null && player.connection == (ClientPacketListener) (Object) this) {
                ibaritone.getGameEventHandler().onChunkEvent(
                        new ChunkEvent(
                                EventState.POST,
                                packetIn.isFullChunk() ? ChunkEvent.Type.POPULATE_FULL : ChunkEvent.Type.POPULATE_PARTIAL,
                                packetIn.getX(),
                                packetIn.getZ()
                        )
                );
            }
        }
    }

    @Inject(
            method = "processChunkUnload",
            at = @At("HEAD")
    )
    private void preChunkUnload(ClientboundForgetLevelChunkPacket packet, CallbackInfo ci) {
        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            LocalPlayer player = ibaritone.getPlayerContext().player();
            if (player != null && player.connection == (ClientPacketListener) (Object) this) {
                ibaritone.getGameEventHandler().onChunkEvent(
                        new ChunkEvent(EventState.PRE, ChunkEvent.Type.UNLOAD, packet.getX(), packet.getZ())
                );
            }
        }
    }

    @Inject(
            method = "processChunkUnload",
            at = @At("RETURN")
    )
    private void postChunkUnload(ClientboundForgetLevelChunkPacket packet, CallbackInfo ci) {
        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            LocalPlayer player = ibaritone.getPlayerContext().player();
            if (player != null && player.connection == (ClientPacketListener) (Object) this) {
                ibaritone.getGameEventHandler().onChunkEvent(
                        new ChunkEvent(EventState.POST, ChunkEvent.Type.UNLOAD, packet.getX(), packet.getZ())
                );
            }
        }
    }

    @Inject(
            method = "handleBlockChange",
            at = @At("RETURN")
    )
    private void postHandleBlockChange(ClientboundBlockUpdatePacket packetIn, CallbackInfo ci) {
        if (!Baritone.settings().repackOnAnyBlockChange.value) {
            return;
        }
        if (!CachedChunk.BLOCKS_TO_KEEP_TRACK_OF.contains(packetIn.getBlockState().getBlock())) {
            return;
        }
        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            LocalPlayer player = ibaritone.getPlayerContext().player();
            if (player != null && player.connection == (ClientPacketListener) (Object) this) {
                ibaritone.getGameEventHandler().onChunkEvent(
                        new ChunkEvent(
                                EventState.POST,
                                ChunkEvent.Type.POPULATE_FULL,
                                packetIn.getPos().getX() >> 4,
                                packetIn.getPos().getZ() >> 4
                        )
                );
            }
        }
    }

    @Inject(
            method = "handleMultiBlockChange",
            at = @At("RETURN")
    )
    private void postHandleMultiBlockChange(ClientboundSectionBlocksUpdatePacket packetIn, CallbackInfo ci) {
        if (!Baritone.settings().repackOnAnyBlockChange.value) {
            return;
        }
        ChunkPos[] chunkPos = new ChunkPos[1];
        packetIn.runUpdates((pos, state) -> {
            if (CachedChunk.BLOCKS_TO_KEEP_TRACK_OF.contains(state.getBlock())) {
                chunkPos[0] = new ChunkPos(pos);
            }
        });
        if (chunkPos[0] == null) {
            return;
        }
        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            LocalPlayer player = ibaritone.getPlayerContext().player();
            if (player != null && player.connection == (ClientPacketListener) (Object) this) {
                ibaritone.getGameEventHandler().onChunkEvent(
                        new ChunkEvent(
                                EventState.POST,
                                ChunkEvent.Type.POPULATE_FULL,
                                chunkPos[0].x,
                                chunkPos[0].z
                        )
                );
            }
        }
    }

    @Inject(
            method = "handleCombatEvent",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/Minecraft.displayGuiScreen(Lnet/minecraft/client/gui/screen/Screen;)V"
            )
    )
    private void onPlayerDeath(ClientboundPlayerCombatPacket packetIn, CallbackInfo ci) {
        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            LocalPlayer player = ibaritone.getPlayerContext().player();
            if (player != null && player.connection == (ClientPacketListener) (Object) this) {
                ibaritone.getGameEventHandler().onPlayerDeath();
            }
        }
    }
}
