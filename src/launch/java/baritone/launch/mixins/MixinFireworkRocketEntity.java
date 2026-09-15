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

import baritone.behavior.LookBehavior;
import baritone.utils.MeteorClientCompat;
import baritone.utils.accessor.IFireworkRocketEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.OptionalInt;

@Mixin(FireworkRocketEntity.class)
public abstract class MixinFireworkRocketEntity extends Entity implements IFireworkRocketEntity {

    @Shadow
    @Final
    private static EntityDataAccessor<OptionalInt> DATA_ATTACHED_TO_TARGET;

    @Shadow
    private LivingEntity attachedToEntity;

    @Shadow
    private int life;

    @Shadow
    private int lifetime;

    @Shadow
    public abstract boolean isAttachedToEntity();

    private MixinFireworkRocketEntity(Level level) {
        super(EntityTypes.FIREWORK_ROCKET, level);
    }

    @Override
    public LivingEntity getBoostedEntity() {
        if (this.isAttachedToEntity() && this.attachedToEntity == null) { // isAttachedToEntity checks if the optional is present
            final Entity entity = this.level().getEntity(this.entityData.get(DATA_ATTACHED_TO_TARGET).getAsInt());
            if (entity instanceof LivingEntity) {
                this.attachedToEntity = (LivingEntity) entity;
            }
        }
        return this.attachedToEntity;
    }

    /**
     * When a firework boosts the local player, the vanilla boost impulse is applied along
     * {@code attachedToEntity.getLookAngle()}. Under elytra free look that is the player's *camera*
     * angle (LookBehavior restores it to the original value after applying Baritone's rotation), so
     * an applied boost yanks the player toward wherever the camera is pointing instead of along
     * Baritone's steering heading. Redirect this call to use Baritone's steering look direction
     * whenever one is active.
     */
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/world/entity/LivingEntity.getLookAngle()Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private Vec3 onGetLookAngle(LivingEntity instance) {
        if (instance == Minecraft.getInstance().player) {
            final Vec3 steering = LookBehavior.getSteeringLookDirection();
            if (steering != null) {
                return steering;
            }
        }
        return instance.getLookAngle();
    }

    /**
     * A Baritone-spawned ghost rocket is a purely client-side entity: the server never sees it, so it
     * never broadcasts the entity event that would make the vanilla client discard it. Those rockets
     * would otherwise keep their boost impulse applied forever while the player is fall-flying. Mirror
     * the server-side expiry (discard once {@code life > lifetime}) for exactly the ghost rockets we
     * spawned, giving them the boost duration implied by the module's firework duration setting.
     */
    @Inject(
            method = "tick",
            at = @At("RETURN")
    )
    private void expireGhostBoostRocket(CallbackInfo ci) {
        if (this.level().isClientSide() && MeteorClientCompat.isGhostRocket(this.getId())) {
            if (this.life > this.lifetime || this.isRemoved()) {
                MeteorClientCompat.untrackGhostRocket(this.getId());
            }
            if (this.life > this.lifetime && !this.isRemoved()) {
                this.discard();
            }
        }
    }
}
