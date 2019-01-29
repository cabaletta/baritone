/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.launch.mixins;

import baritone.api.BaritoneAPI;
import baritone.utils.Helper;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Brady
 * @since 7/31/2018
 */
@Mixin(KeyBinding.class)
public class MixinKeyBinding {

    @Shadow
    private int pressTime;

    @Inject(
            method = "isKeyDown",
            at = @At("HEAD"),
            cancellable = true
    )
    private void isKeyDown(CallbackInfoReturnable<Boolean> cir) {
        // only the primary baritone forces keys
        Boolean force = BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().isInputForcedDown((KeyBinding) (Object) this);
        if (force != null) {
            cir.setReturnValue(force); // :sunglasses:
        }
    }

    @Inject(
            method = "isPressed",
            at = @At("HEAD"),
            cancellable = true
    )
    private void isPressed(CallbackInfoReturnable<Boolean> cir) {
        // only the primary baritone forces keys
        Boolean force = BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().isInputForcedDown((KeyBinding) (Object) this);
        if (force != null && !force) { // <-- cursed
            if (pressTime > 0) {
                Helper.HELPER.logDirect("You're trying to press this mouse button but I won't let you");
                pressTime--;
            }
            cir.setReturnValue(force); // :sunglasses:
        }
    }
}
