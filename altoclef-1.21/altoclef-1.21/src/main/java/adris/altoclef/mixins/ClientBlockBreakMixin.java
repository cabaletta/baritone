package adris.altoclef.mixins;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.BlockBreakingCancelEvent;
import adris.altoclef.eventbus.events.BlockBreakingEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public final class ClientBlockBreakMixin {

    // for SOME REASON baritone triggers a block cancel breaking every other frame, so we have a 2 frame requirement for that?
    private static int _breakCancelFrames;

    @Inject(
            method = "updateBlockBreakingProgress",
            at = @At("HEAD")
    )
    private void onBreakUpdate(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> ci) {
        ClientBlockBreakAccessor breakAccessor = (ClientBlockBreakAccessor) (MinecraftClient.getInstance().interactionManager);
        if (breakAccessor != null) {
            _breakCancelFrames = 2;
            EventBus.publish(new BlockBreakingEvent(pos, breakAccessor.getCurrentBreakingProgress()));
        }
    }

    @Inject(
            method = "cancelBlockBreaking",
            at = @At("HEAD")
    )
    private void cancelBlockBreaking(CallbackInfo ci) {
        if (_breakCancelFrames-- == 0) {
            EventBus.publish(new BlockBreakingCancelEvent());
        }
    }
}
