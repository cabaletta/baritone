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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

import static org.objectweb.asm.Opcodes.GETFIELD;

/**
 * @author Brady
 * @since 9/10/2018
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {

    /**
     * Event called to override the movement direction when jumping
     */
    @Unique
    private RotationMoveEvent jumpRotationEvent;

    @Unique
    private RotationMoveEvent elytraRotationEvent;

    public MixinLivingEntity(EntityType<?> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Inject(
            method = "jumpFromGround",
            at = @At("HEAD")
    )
    private void preMoveRelative(CallbackInfo ci) {
        this.getBaritone().ifPresent(baritone -> {
            this.jumpRotationEvent = new RotationMoveEvent(RotationMoveEvent.Type.JUMP, this.getYRot(), this.getXRot());
            baritone.getGameEventHandler().onPlayerRotationMove(this.jumpRotationEvent);
        });
    }

    @Redirect(
            method = "jumpFromGround",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/world/entity/LivingEntity.getYRot()F"
            )
    )
    private float overrideYaw(LivingEntity self) {
        if (self instanceof LocalPlayer && BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) (Object) this) != null) {
            return this.jumpRotationEvent.getYaw();
        }
        return self.getYRot();
    }

    @Inject(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/world/entity/LivingEntity.getLookAngle()Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private void onPreElytraMove(Vec3 direction, CallbackInfo ci) {
        this.getBaritone().ifPresent(baritone -> {
            this.elytraRotationEvent = new RotationMoveEvent(RotationMoveEvent.Type.MOTION_UPDATE, this.getYRot(), this.getXRot());
            baritone.getGameEventHandler().onPlayerRotationMove(this.elytraRotationEvent);
            this.setYRot(this.elytraRotationEvent.getYaw());
            this.setXRot(this.elytraRotationEvent.getPitch());
        });
    }

    @Inject(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/world/entity/Entity.move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onPostElytraMove(float strafe, float vertical, float forward, CallbackInfo ci) {
        if (this.elytraRotationEvent != null) {
            this.setYRot(this.elytraRotationEvent.getOriginal().getYaw());
            this.setXRot(this.elytraRotationEvent.getOriginal().getPitch());
            this.elytraRotationEvent = null;
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
