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
import baritone.api.IBaritone;
import baritone.api.event.events.RotationMoveEvent;
import baritone.api.utils.Rotation;
import baritone.behavior.LookBehavior;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {

    @Shadow
    private float yRot;

    @Shadow
    private float xRot;

    @Unique
    private RotationMoveEvent motionUpdateRotationEvent;

    @Inject(
            method = "moveRelative",
            at = @At("HEAD")
    )
    private void moveRelativeHead(CallbackInfo info) {
        // noinspection ConstantConditions
        if (!LocalPlayer.class.isInstance(this) || BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) (Object) this) == null) {
            return;
        }
        this.motionUpdateRotationEvent = new RotationMoveEvent(RotationMoveEvent.Type.MOTION_UPDATE, this.yRot, this.xRot);
        BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) (Object) this).getGameEventHandler().onPlayerRotationMove(motionUpdateRotationEvent);
        this.yRot = this.motionUpdateRotationEvent.getYaw();
        this.xRot = this.motionUpdateRotationEvent.getPitch();
    }

    @Inject(
            method = "moveRelative",
            at = @At("RETURN")
    )
    private void moveRelativeReturn(CallbackInfo info) {
        if (this.motionUpdateRotationEvent != null) {
            this.yRot = this.motionUpdateRotationEvent.getOriginal().getYaw();
            this.xRot = this.motionUpdateRotationEvent.getOriginal().getPitch();
            this.motionUpdateRotationEvent = null;
        }
    }

    @Inject(
            method = "getViewYRot",
            at = @At("HEAD"),
            cancellable = true
    )
    private void getViewYRot(float partialTicks, CallbackInfoReturnable<Float> cir) {
        Rotation visualRotation = this.getVisualRotation(partialTicks);
        if (visualRotation != null) {
            cir.setReturnValue(visualRotation.getYaw());
        }
    }

    @Inject(
            method = "getViewXRot",
            at = @At("HEAD"),
            cancellable = true
    )
    private void getViewXRot(float partialTicks, CallbackInfoReturnable<Float> cir) {
        Rotation visualRotation = this.getVisualRotation(partialTicks);
        if (visualRotation != null) {
            cir.setReturnValue(visualRotation.getPitch());
        }
    }

    @Unique
    private Rotation getVisualRotation(float partialTicks) {
        if (!LocalPlayer.class.isInstance(this)) {
            return null;
        }
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) (Object) this);
        if (baritone == null) {
            return null;
        }
        return ((LookBehavior) baritone.getLookBehavior()).getVisualRotation(partialTicks);
    }
}
