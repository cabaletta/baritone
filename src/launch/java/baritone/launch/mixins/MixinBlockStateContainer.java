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

import baritone.utils.accessor.IBitArray;
import baritone.utils.accessor.IBlockStateContainer;
import net.minecraft.util.BitArray;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraft.world.chunk.IBlockStatePalette;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockStateContainer.class)
public abstract class MixinBlockStateContainer<T> implements IBlockStateContainer<T> {

    @Shadow
    protected BitArray storage;

    @Shadow
    protected IBlockStatePalette<T> palette;

    @Override
    public IBlockStatePalette<T> getPalette() {
        return palette;
    }

    @Override
    public BitArray getStorage() {
        return storage;
    }

    @Override
    public T getAtPalette(int index) {
        return palette.get(index);
    }

    @Override
    public int[] storageArray() {
        return ((IBitArray) storage).toArray();
    }
}
