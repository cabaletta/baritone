package baritone.launch.mixins;

import baritone.bot.Baritone;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Brady
 * @since 8/1/2018 12:28 AM
 */
@Mixin(GameSettings.class)
public class MixinGameSettings {

    @Redirect(
            method = "isKeyDown",
            at = @At(
                    value = "INVOKE",
                    target = "org/lwjgl/input/Keyboard.isKeyDown(I)Z"
            )
    )
    private static boolean isKeyDown(int keyCode) {
        return Baritone.INSTANCE.getInputOverrideHandler().isKeyDown(keyCode);
    }
}
