/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

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
