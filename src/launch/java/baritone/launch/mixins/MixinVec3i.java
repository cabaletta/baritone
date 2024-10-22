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

import baritone.api.utils.BetterBlockPos;
import net.minecraft.core.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Vec3i.class)
public class MixinVec3i {

    /**
     * @author Babbaj
     * @reason overriding hashCode with different behavior violates the general contract of hashCode.
     * 2 BlockPos objects that are equal but give different hashCodes trolls hashmaps and causes duplicate entries.
     */
    @Overwrite
    public int hashCode() {
        Vec3i vec = (Vec3i) (Object) this;
        return (int) BetterBlockPos.longHash(vec.getX(), vec.getY(), vec.getZ());
    }
}
