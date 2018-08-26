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

package baritone.launch.mixins;

import com.google.common.base.MoreObjects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Brady
 * @since 8/25/2018
 */
@Mixin(Vec3i.class)
public class MixinVec3i {

    @Redirect(
            method = "toString",
            at = @At(
                    value = "INVOKE",
                    target = "com/google/common/base/MoreObjects.toStringHelper(Ljava/lang/Object;)Lcom/google/common/base/MoreObjects$ToStringHelper;"
            )
    )
    private MoreObjects.ToStringHelper toStringHelper(Object object) {

        if (object.getClass().equals(BlockPos.class)) {
            return MoreObjects.toStringHelper("BlockPos");
        }

        return MoreObjects.toStringHelper(object);
    }
}
