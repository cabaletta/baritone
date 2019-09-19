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

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.properties.IProperty;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.block.state.BlockStateContainer$StateImplementation")
public abstract class MixinStateImplementation {

    @Shadow
    @Final
    private ImmutableMap<IProperty<?>, Comparable<?>> properties;

    /**
     * Block states are fucking immutable
     */
    @Unique
    private int hashCode;

    @Inject(
            method = "<init>*",
            at = @At("RETURN")
    )
    private void onInit(CallbackInfo ci) {
        hashCode = properties.hashCode();
    }

    /**
     * Cache this instead of using the fucking map every time
     *
     * @author LoganDark
     * @reason Regular IBlockState generates a new hash every fucking time. This is not needed when scanning millions
     * per second
     */
    @Override
    @Overwrite
    public int hashCode() {
        return hashCode;
    }
}
