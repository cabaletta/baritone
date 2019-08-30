package baritone.launch.mixins;

import net.minecraft.client.gui.GuiChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.ChatTabCompleter.class)
public abstract class MixinChatTabCompleter extends MixinTabCompleter {
    @Inject(method = "<init>*", at = @At("RETURN"))
    private void onConstruction(CallbackInfo ci) {
        isChatCompleter = true;
    }

    @Inject(method = "complete", at = @At("HEAD"), cancellable = true)
    private void onComplete(CallbackInfo ci) {
        if (dontComplete) ci.cancel();
    }
}
