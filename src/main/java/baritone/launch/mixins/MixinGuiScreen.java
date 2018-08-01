package baritone.launch.mixins;

import baritone.bot.Baritone;
import baritone.util.ChatCommand;
import net.minecraft.client.gui.GuiScreen;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.InvocationTargetException;

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

    @Inject(method="sendChatMessage",at=@At("HEAD"))
    public void sendChatMessage(String msg, CallbackInfo cir)
    {
        try {
            ChatCommand.message(msg);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
