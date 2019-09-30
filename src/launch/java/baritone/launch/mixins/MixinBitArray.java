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
import net.minecraft.util.BitArray;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BitArray.class)
public abstract class MixinBitArray implements IBitArray {

    @Shadow
    @Final
    private long[] longArray;

    @Shadow
    @Final
    private int bitsPerEntry;

    @Shadow
    @Final
    private long maxEntryValue;

    @Shadow
    @Final
    private int arraySize;

    /**
     * why did mojang divide by 64 instead of shifting right by 6 (2^6=64)?
     * why did mojang modulo by 64 instead of ANDing with 63?
     * also removed validation check
     *
     * @author LoganDark
     */
    @Override
    @Unique
    public int getAtFast(int index) {
        final int b = bitsPerEntry;
        final long mev = maxEntryValue;
        final int i = index * b;
        final int j = i >> 6;
        final int l = i & 63;
        final int k = ((index + 1) * b - 1) >> 6;

        if (j == k) {
            return (int) (this.longArray[j] >>> l & mev);
        } else {
            return (int) ((this.longArray[j] >>> l | longArray[k] << (64 - l)) & mev);
        }
    }

    @Override
    @Unique
    public int[] toArray() {
        int[] out = new int[arraySize];

        for (int idx = 0, kl = bitsPerEntry - 1; idx < arraySize; idx++, kl += bitsPerEntry) {
            final int i = idx * bitsPerEntry;
            final int j = i >> 6;
            final int l = i & 63;
            final int k = kl >> 6;
            final long jl = longArray[j] >>> l;

            if (j == k) {
                out[idx] = (int) (jl & maxEntryValue);
            } else {
                out[idx] = (int) ((jl | longArray[k] << (64 - l)) & maxEntryValue);
            }
        }

        return out;
    }
}
