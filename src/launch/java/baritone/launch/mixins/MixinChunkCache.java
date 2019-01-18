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

import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.utils.IPlayerContext;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkCache.class)
public class MixinChunkCache {
    @Inject(
            method = "getBlockState",
            at = @At("HEAD"),
            cancellable = true
    )
    private void getBlockState(BlockPos pos, CallbackInfoReturnable<IBlockState> ci) {
        Baritone baritone = (Baritone) BaritoneAPI.getProvider().getPrimaryBaritone();
        IPlayerContext ctx = baritone.getPlayerContext();
        if (ctx.player() != null && ctx.world() != null && baritone.bsi != null) {
            ci.setReturnValue(baritone.bsi.get0(pos));
            //ci.setReturnValue(Blocks.DIRT.getDefaultState());
        }
    }
}
