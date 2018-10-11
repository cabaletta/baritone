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

import baritone.Baritone;
import baritone.api.event.events.RotationMoveEvent;
import baritone.api.event.events.type.EventState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Brady
 * @since 9/10/2018
 */
@Mixin(EntityLivingBase.class)
public class MixinEntityLivingBase {

    @Inject(
            method = "jump",
            at = @At("HEAD")
    )
    private void preJump(CallbackInfo ci) {
        EntityLivingBase _this = (EntityLivingBase) (Object) this;
        if (EntityPlayerSP.class.isInstance(_this))
            Baritone.INSTANCE.getGameEventHandler().onPlayerRotationMove(new RotationMoveEvent((EntityPlayerSP) _this, EventState.PRE, RotationMoveEvent.Type.JUMP));
    }

    @Inject(
            method = "jump",
            at = @At("RETURN")
    )
    private void postJump(CallbackInfo ci) {
        EntityLivingBase _this = (EntityLivingBase) (Object) this;
        if (EntityPlayerSP.class.isInstance(_this))
            Baritone.INSTANCE.getGameEventHandler().onPlayerRotationMove(new RotationMoveEvent((EntityPlayerSP) _this, EventState.POST, RotationMoveEvent.Type.JUMP));
    }
}
