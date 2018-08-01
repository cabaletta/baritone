package baritone.launch.mixins;

import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Brady
 * @since 7/31/2018 10:18 PM
 */
@Mixin(Main.class)
public class MixinMain {

    @Inject(
            method = "main",
            at = @At("HEAD")
    )
    private static void main(String[] args, CallbackInfo ci) {
        System.setProperty("java.net.preferIPv4Stack", "true");
    }
}
