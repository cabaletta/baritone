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

package baritone.process;

import baritone.Baritone;
import baritone.api.process.IAttackProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class AttackProcess extends BaritoneProcessHelper implements IAttackProcess {
    private boolean rotating = false;
    private boolean attacking = false;

    public AttackProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public boolean isActive() {
        this.rotating = false;
        this.attacking = false;
        return ctx.player() != null &&
                ctx.world() != null &&
                Baritone.settings().entityAttackRadius.value != 0.0 &&
                (this.baritone.getFollowProcess().isActive() || this.baritone.getPathingBehavior().isPathing());
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        this.baritone.getInputOverrideHandler().clearAllKeys();
        double closestDistance = Double.MAX_VALUE;
        Vec3 closestPosition = null;
        for (Entity entity : ctx.entities()) {
            if (!entity.is(ctx.player()) && entity instanceof LivingEntity && entity.isAlive() && entity.isAttackable()) {
                Vec3 attackPoint = new Vec3(
                        entity.getBoundingBox().getCenter().x(),//Mth.clamp(ctx.playerHead().x(), entity.getBoundingBox().minX, entity.getBoundingBox().maxX),
                        Mth.clamp(ctx.playerHead().y(), entity.getBoundingBox().minY, entity.getBoundingBox().maxY),
                        entity.getBoundingBox().getCenter().z()//Mth.clamp(ctx.playerHead().z(), entity.getBoundingBox().minZ, entity.getBoundingBox().maxZ)
                );
                double distance = this.ctx.player().getEyePosition().distanceToSqr(attackPoint);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPosition = attackPoint;
                }
            }
        }
        double attackRadius = Baritone.settings().entityAttackRadius.value;
        if (closestPosition != null && closestDistance <= attackRadius * attackRadius) {
            if (!Baritone.settings().assumeExternalAutoAim.value) {
                this.rotating = true;
                this.baritone.getLookBehavior().updateTarget(
                        RotationUtils.calcRotationFromVec3d(this.ctx.playerHead(), closestPosition, this.ctx.playerRotations()),
                        true
                );
            }
            if (!Baritone.settings().assumeExternalKillAura.value) {
                HitResult hitResult = ctx.minecraft().hitResult;
                if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
                    this.attacking = true;
                }
            }
        }
        return new PathingCommand(null, PathingCommandType.DEFER);
    }

    @Override
    public void onLostControl() {}

    @Override
    public String displayName0() {
        return "Attack";
    }

    @Override
    public boolean isTemporary() {
        return true;
    }

    @Override
    public double priority() {
        return 5.0;
    }

    @Override
    public boolean isRotating() {
        return this.rotating;
    }

    @Override
    public boolean isAttacking() {
        return this.attacking;
    }
}
