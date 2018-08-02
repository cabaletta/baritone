package baritone.launch.mixins;

import baritone.util.ChatCommand;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.InvocationTargetException;

/**
 * @author Brady
 * @since 8/1/2018 5:06 PM
 */
@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {

    @Inject(
            method = "sendChatMessage",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sendChatMessage(String msg, CallbackInfo ci) {
        try {
            if (ChatCommand.message(msg))
                ci.cancel();
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
