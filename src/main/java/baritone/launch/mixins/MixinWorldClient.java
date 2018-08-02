package baritone.launch.mixins;

import baritone.bot.Baritone;
import baritone.bot.event.events.ChunkEvent;
import baritone.bot.event.events.type.EventState;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Brady
 * @since 8/2/2018 12:41 AM
 */
@Mixin(WorldClient.class)
public class MixinWorldClient {

    @Inject(
            method = "doPreChunk",
            at = @At("HEAD")
    )
    private void preDoPreChunk(int chunkX, int chunkY, boolean loadChunk, CallbackInfo ci) {
        Baritone.INSTANCE.getGameEventHandler().onChunkEvent(
                new ChunkEvent(
                        EventState.PRE,
                        loadChunk ? ChunkEvent.Type.LOAD : ChunkEvent.Type.UNLOAD,
                        chunkX,
                        chunkY
                )
        );
    }

    @Inject(
            method = "doPreChunk",
            at = @At("RETURN")
    )
    private void postDoPreChunk(int chunkX, int chunkY, boolean loadChunk, CallbackInfo ci) {
        Baritone.INSTANCE.getGameEventHandler().onChunkEvent(
                new ChunkEvent(
                        EventState.POST,
                        loadChunk ? ChunkEvent.Type.LOAD : ChunkEvent.Type.UNLOAD,
                        chunkX,
                        chunkY
                )
        );
    }
}
