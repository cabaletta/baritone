package baritone.launch.mixins;

import baritone.bot.Baritone;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.IntHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Brady
 * @since 7/31/2018 11:44 PM
 */
@Mixin(KeyBinding.class)
public abstract class MixinKeyBinding {

    @Redirect(
            method = "onTick",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/util/IntHashMap.lookup(I)Ljava/lang/Object;"
            )
    )
    private static Object lookup(IntHashMap<KeyBinding> HASH, int keyCode) {
        KeyBinding keyBinding = HASH.lookup(keyCode);

        // If we're overriding the key state, we don't want to be incrementing the pressTime
        if (keyBinding != null && Baritone.INSTANCE.getInputOverrideHandler().isInputForcedDown(keyBinding))
            return null;

        return keyBinding;
    }

    @Inject(
            method = "isPressed",
            at = @At("HEAD"),
            cancellable = true
    )
    private void isPressed(CallbackInfoReturnable<Boolean> cir) {
        if (Baritone.INSTANCE.getInputOverrideHandler().isInputForcedDown((KeyBinding) (Object) this))
            cir.setReturnValue(true);
    }

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
