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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * While swimming, the vertical part of your motion is pulled towards your look angle in
 * {@code Player.travel}, which reads the real entity rotation — outside of {@code moveRelative}, so the
 * moveRelative rotation spoof can't cover it. Same deal as elytra flight in {@link MixinLivingEntity}:
 * set the real rotation for the duration of the getLookAngle call so pitch steering works with free look,
 * then restore. The user's camera never sees any of this.
 */
@Mixin(Player.class)
public abstract class MixinPlayer extends Entity {

    /**
     * Event called to override the movement direction while swimming
     */
    @Unique
    private RotationMoveEvent swimRotationEvent;

    private MixinPlayer(EntityType<?> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getLookAngle()Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private void preSwimMove(Vec3 direction, CallbackInfo ci) {
        this.getBaritone().ifPresent(baritone -> {
            this.swimRotationEvent = new RotationMoveEvent(RotationMoveEvent.Type.MOTION_UPDATE, this.getYRot(), this.getXRot());
            baritone.getGameEventHandler().onPlayerRotationMove(this.swimRotationEvent);
            this.setYRot(this.swimRotationEvent.getYaw());
            this.setXRot(this.swimRotationEvent.getPitch());
        });
    }

    @Inject(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getLookAngle()Lnet/minecraft/world/phys/Vec3;",
                    shift = At.Shift.AFTER
            )
    )
    private void postSwimAngle(Vec3 direction, CallbackInfo ci) {
        if (this.swimRotationEvent != null) {
            this.setYRot(this.swimRotationEvent.getOriginal().getYaw());
            this.setXRot(this.swimRotationEvent.getOriginal().getPitch());
            this.swimRotationEvent = null;
        }
    }

    @Unique
    private Optional<IBaritone> getBaritone() {
        // noinspection ConstantConditions
        if (LocalPlayer.class.isInstance(this)) {
            return Optional.ofNullable(BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) (Object) this));
        } else {
            return Optional.empty();
        }
    }
}
