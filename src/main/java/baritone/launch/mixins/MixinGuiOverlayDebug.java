package baritone.launch.mixins;

import baritone.bot.HookStateManager;
import baritone.bot.Baritone;
import net.minecraft.client.gui.GuiOverlayDebug;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * @author Brady
 * @since 7/31/2018 10:28 PM
 */
@Mixin(GuiOverlayDebug.class)
public abstract class MixinGuiOverlayDebug {

    @Shadow protected abstract void renderDebugInfoRight(ScaledResolution scaledResolution);

    @Redirect(
            method = "renderDebugInfo",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/gui/GuiOverlayDebug.renderDebugInfoRight(Lnet/minecraft/client/gui/ScaledResolution;)V"
            )
    )
    private void onRenderDebugInfoRight(GuiOverlayDebug gui, ScaledResolution scaledResolution) {
        if (!Baritone.INSTANCE.getHookStateManager().shouldCancelDebugRenderRight()) {
            this.renderDebugInfoRight(scaledResolution);
        }
    }

    @Inject(
            method = "call",
            at = @At("HEAD"),
            cancellable = true
    )
    private void call(CallbackInfoReturnable<List<String>> cir) {
        HookStateManager hooks = Baritone.INSTANCE.getHookStateManager();

        if (hooks.shouldOverrideDebugInfoLeft()) {
            cir.setReturnValue(hooks.getDebugInfoLeft());
        }
    }
}
