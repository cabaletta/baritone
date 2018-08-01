package baritone.launch.mixins;

import baritone.bot.Baritone;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Brady
 * @since 7/31/2018 10:38 PM
 */
@Mixin(GuiScreen.class)
public class MixinGuiScreen {

    @Redirect(
            method = {
                    "isCtrlKeyDown",
                    "isShiftKeyDown",
                    "isAltKeyDown"
            },
            at = @At(
                    value = "INVOKE",
                    target = "org/lwjgl/input/Keyboard.isKeyDown(I)Z"
            )
    )
    private static boolean isKeyDown(int keyCode) {
        return Baritone.INSTANCE.getInputOverrideHandler().isKeyDown(keyCode);
    }
}
