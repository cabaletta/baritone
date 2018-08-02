package baritone.launch.mixins;

import baritone.bot.Baritone;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Brady
 * @since 7/31/2018 11:44 PM
 */
@Mixin(KeyBinding.class)
public abstract class MixinKeyBinding {

    @Inject(
            method = "isKeyDown",
            at = @At("HEAD"),
            cancellable = true
    )
    private void isKeyDown(CallbackInfoReturnable<Boolean> cir) {
        if (Baritone.INSTANCE.getInputOverrideHandler().isInputForcedDown((KeyBinding) (Object) this))
            cir.setReturnValue(true);
    }
}
