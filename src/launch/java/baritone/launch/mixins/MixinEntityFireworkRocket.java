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

import baritone.utils.accessor.IEntityFireworkRocket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityFireworkRocket.class)
public abstract class MixinEntityFireworkRocket extends Entity implements IEntityFireworkRocket {

    @Shadow
    @Final
    private static DataParameter<Integer> BOOSTED_ENTITY_ID;

    @Shadow
    private EntityLivingBase boostedEntity;

    @Shadow
    public abstract boolean isAttachedToEntity();

    private MixinEntityFireworkRocket(World worldIn) {
        super(worldIn);
    }

    @Override
    public EntityLivingBase getBoostedEntity() {
        if (this.isAttachedToEntity() && this.boostedEntity == null) {
            final Entity entity = this.world.getEntityByID(this.dataManager.get(BOOSTED_ENTITY_ID));
            if (entity instanceof EntityLivingBase) {
                this.boostedEntity = (EntityLivingBase) entity;
            }
        }
        return this.boostedEntity;
    }
}
