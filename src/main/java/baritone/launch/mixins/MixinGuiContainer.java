package baritone.launch.mixins;

import baritone.bot.Baritone;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Brady
 * @since 7/31/2018 10:47 PM
 */
@Mixin(GuiContainer.class)
public class MixinGuiContainer {

    @Redirect(
            method = {
                    "mouseClicked",
                    "mouseReleased"
            },
            at = @At(
                    value = "INVOKE",
                    target = "org/lwjgl/input/Keyboard.isKeyDown(I)Z"
            )
    )
    private boolean isKeyDown(int keyCode) {
        return Baritone.INSTANCE.getInputOverrideHandler().isKeyDown(keyCode);
    }
}
