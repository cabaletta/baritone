/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

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
