package baritone.launch.mixins;

import baritone.utils.accessor.ITabCompleter;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.util.TabCompleter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.class)
public abstract class MixinGuiChat implements net.minecraft.util.ITabCompleter {
    @Shadow
    private TabCompleter tabCompleter;

    @Inject(method = "setCompletions", at = @At("HEAD"), cancellable = true)
    private void onSetCompletions(String[] newCompl, CallbackInfo ci) {
        if (((ITabCompleter) tabCompleter).onGuiChatSetCompletions(newCompl)) {
            ci.cancel();
        }
    }
}
