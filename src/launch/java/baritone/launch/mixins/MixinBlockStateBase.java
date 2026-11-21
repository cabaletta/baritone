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

import baritone.utils.accessor.IBlockStateFlags;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

// one int on every blockstate for PrecomputedData to scribble in. a field load beats a hash map lookup
@Mixin(BlockBehaviour.BlockStateBase.class)
public class MixinBlockStateBase implements IBlockStateFlags {

    @Unique
    private int baritone$pathingFlags;

    @Override
    public int baritone$getPathingFlags() {
        return baritone$pathingFlags;
    }

    @Override
    public void baritone$setPathingFlags(int flags) {
        baritone$pathingFlags = flags;
    }
}
