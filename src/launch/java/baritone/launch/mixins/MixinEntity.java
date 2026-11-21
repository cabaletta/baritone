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
import baritone.api.Settings;
import baritone.api.event.events.RotationMoveEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Redirect(
            method = "updateSwimming",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;isUnderWater()Z"
            )
    )
    private boolean allowSwimStart(Entity self) {
        // vanilla only latches the swim state once the eye is underwater, which at the surface means
        // bobbing around waiting for gravity (water sink speed is 0.005/tick, it takes forever).
        // while baritone is actively swimming, pretend the eye is already under so the swim state
        // latches immediately and the pitch steering dives right away
        if (LocalPlayer.class.isInstance(self)) {
            IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) self);
            if (baritone != null && BaritoneAPI.getSettings().allowSwimming.value
                    && baritone.getPathingBehavior().isPathing()
                    && self.isInWater() && self.isSprinting()) {
                return true;
            }
        }
        return self.isUnderWater();
    }
}
