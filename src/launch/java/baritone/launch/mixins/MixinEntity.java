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
import baritone.api.event.events.RotationMoveEvent;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class MixinEntity {

    @Shadow
    private float rotationYaw;

    @Shadow
    private float rotationPitch;

    @Unique
    private RotationMoveEvent motionUpdateRotationEvent;

    @Inject(
            method = "moveRelative",
            at = @At("HEAD")
    )
    private void moveRelativeHead(CallbackInfo info) {
        // noinspection ConstantConditions
        if (!ClientPlayerEntity.class.isInstance(this) || BaritoneAPI.getProvider().getBaritoneForPlayer((ClientPlayerEntity) (Object) this) == null) {
            return;
        }
        this.motionUpdateRotationEvent = new RotationMoveEvent(RotationMoveEvent.Type.MOTION_UPDATE, this.rotationYaw, this.rotationPitch);
        BaritoneAPI.getProvider().getBaritoneForPlayer((ClientPlayerEntity) (Object) this).getGameEventHandler().onPlayerRotationMove(motionUpdateRotationEvent);
        this.rotationYaw = this.motionUpdateRotationEvent.getYaw();
        this.rotationPitch = this.motionUpdateRotationEvent.getPitch();
    }

    @Inject(
            method = "moveRelative",
            at = @At("RETURN")
    )
    private void moveRelativeReturn(CallbackInfo info) {
        if (this.motionUpdateRotationEvent != null) {
            this.rotationYaw = this.motionUpdateRotationEvent.getOriginal().getYaw();
            this.rotationPitch = this.motionUpdateRotationEvent.getOriginal().getPitch();
            this.motionUpdateRotationEvent = null;
        }
    }
}
