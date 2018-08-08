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

package baritone.launch.mixins;

import baritone.bot.Baritone;
import baritone.bot.event.events.ChunkEvent;
import baritone.bot.event.events.type.EventState;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketChunkData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Brady
 * @since 8/3/2018 12:54 AM
 */
@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    @Inject(
            method = "handleChunkData",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/world/chunk/Chunk.read(Lnet/minecraft/network/PacketBuffer;IZ)V"
            )
    )
    private void preRead(SPacketChunkData packetIn, CallbackInfo ci) {
        Baritone.INSTANCE.getGameEventHandler().onChunkEvent(
                new ChunkEvent(
                        EventState.PRE,
                        ChunkEvent.Type.POPULATE,
                        packetIn.getChunkX(),
                        packetIn.getChunkZ()
                )
        );
    }

    @Inject(
            method = "handleChunkData",
            at = @At("RETURN")
    )
    private void postHandleChunkData(SPacketChunkData packetIn, CallbackInfo ci) {
        Baritone.INSTANCE.getGameEventHandler().onChunkEvent(
                new ChunkEvent(
                        EventState.POST,
                        ChunkEvent.Type.POPULATE,
                        packetIn.getChunkX(),
                        packetIn.getChunkZ()
                )
        );
    }
}
