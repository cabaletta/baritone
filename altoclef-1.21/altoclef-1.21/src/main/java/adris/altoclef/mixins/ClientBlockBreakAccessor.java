package adris.altoclef.mixins;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientBlockBreakAccessor {
    @Accessor("currentBreakingProgress")
    float getCurrentBreakingProgress();
}
