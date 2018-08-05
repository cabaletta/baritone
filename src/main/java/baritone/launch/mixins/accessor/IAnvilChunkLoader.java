package baritone.launch.mixins.accessor;

import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.io.File;

/**
 * @author Brady
 * @since 8/4/2018 11:36 AM
 */
@Mixin(AnvilChunkLoader.class)
public interface IAnvilChunkLoader {

    @Accessor File getChunkSaveLocation();
}
