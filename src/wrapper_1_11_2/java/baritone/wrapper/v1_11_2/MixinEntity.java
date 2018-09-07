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

package baritone.wrapper.v1_11_2;

import baritone.Baritone;
import baritone.api.event.events.RelativeMoveEvent;
import baritone.api.event.events.type.EventState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Brady
 * @since 8/21/2018
 */
@Mixin(Entity.class)
public class MixinEntity {

    @Inject(
            method = "func_70060_a",
            at = @At("HEAD")
    )
    private void preMoveRelative(float strafe, float forward, float friction, CallbackInfo ci) {
        Entity _this = (Entity) (Object) this;
        if (_this == Minecraft.getMinecraft().player)
            Baritone.INSTANCE.getGameEventHandler().onPlayerRelativeMove(new RelativeMoveEvent(EventState.PRE));
    }

    @Inject(
            method = "func_70060_a",
            at = @At("RETURN")
    )
    private void postMoveRelative(float strafe, float forward, float friction, CallbackInfo ci) {
        Entity _this = (Entity) (Object) this;
        if (_this == Minecraft.getMinecraft().player)
            Baritone.INSTANCE.getGameEventHandler().onPlayerRelativeMove(new RelativeMoveEvent(EventState.POST));
    }
}
