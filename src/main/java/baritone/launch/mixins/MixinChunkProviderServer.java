package baritone.launch.mixins;

import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author Brady
 * @since 8/4/2018 11:33 AM
 */
@Mixin(ChunkProviderServer.class)
public interface MixinChunkProviderServer {

    @Accessor IChunkLoader getChunkLoader();
}
