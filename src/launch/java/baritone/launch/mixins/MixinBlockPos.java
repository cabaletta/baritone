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

import com.google.common.base.MoreObjects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.Nonnull;

/**
 * @author Brady
 * @since 8/25/2018
 */
@Mixin(BlockPos.class)
public class MixinBlockPos extends Vec3i {

    public MixinBlockPos(int xIn, int yIn, int zIn) {
        super(xIn, yIn, zIn);
    }

    /**
     * The purpose of this was to ensure a friendly name for when we print raw
     * block positions to chat in the context of an obfuscated environment.
     *
     * @return a string representation of the object.
     */
    @Override
    @Nonnull
    public String toString() {
        return MoreObjects.toStringHelper("BlockPos").add("x", this.getX()).add("y", this.getY()).add("z", this.getZ()).toString();
    }
}
