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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    private RotationMoveEvent jumpRotationEvent;

    public MixinLivingEntity(EntityType<?> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Inject(
            method = "jumpFromGround",
            at = @At("HEAD")
    )
    private void preMoveRelative(CallbackInfo ci) {
        // noinspection ConstantConditions
        if (LocalPlayer.class.isInstance(this)) {
            IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) (Object) this);
            if (baritone != null) {
                this.jumpRotationEvent = new RotationMoveEvent(RotationMoveEvent.Type.JUMP, this.getYRot());
                baritone.getGameEventHandler().onPlayerRotationMove(this.jumpRotationEvent);
            }
        }
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


}
