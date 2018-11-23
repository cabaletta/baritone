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

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.event.events.ChunkEvent;
import baritone.api.event.events.type.EventState;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Brady
 * @since 8/2/2018
 */
@Mixin(WorldClient.class)
public class MixinWorldClient {

    @Inject(
            method = "doPreChunk",
            at = @At("HEAD")
    )
    private void preDoPreChunk(int chunkX, int chunkZ, boolean loadChunk, CallbackInfo ci) {
        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            if (ibaritone.getPlayerContext().world() == (WorldClient) (Object) this) {
                ibaritone.getGameEventHandler().onChunkEvent(
                        new ChunkEvent(
                                EventState.PRE,
                                loadChunk ? ChunkEvent.Type.LOAD : ChunkEvent.Type.UNLOAD,
                                chunkX,
                                chunkZ
                        )
                );
            }
        }

    }

    @Inject(
            method = "doPreChunk",
            at = @At("RETURN")
    )
    private void postDoPreChunk(int chunkX, int chunkZ, boolean loadChunk, CallbackInfo ci) {
        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            if (ibaritone.getPlayerContext().world() == (WorldClient) (Object) this) {
                ibaritone.getGameEventHandler().onChunkEvent(
                        new ChunkEvent(
                                EventState.POST,
                                loadChunk ? ChunkEvent.Type.LOAD : ChunkEvent.Type.UNLOAD,
                                chunkX,
                                chunkZ
                        )
                );
            }
        }
    }
}
