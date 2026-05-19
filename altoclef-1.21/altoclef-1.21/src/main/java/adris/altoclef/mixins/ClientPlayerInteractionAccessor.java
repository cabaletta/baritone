package adris.altoclef.mixins;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionAccessor {
    //@Invoker("sendPlayerAction")
    //void doSendPlayerAction(PlayerActionC2SPacket.Action action, BlockPos pos, Direction direction);

}
