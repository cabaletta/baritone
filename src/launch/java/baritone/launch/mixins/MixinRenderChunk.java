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
import net.minecraft.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.chunk.RenderChunkCache;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Brady
 * @since 1/29/2019
 */
@Mixin(RenderChunk.class)
public class MixinRenderChunk {

    @Redirect(
            method = "rebuildChunk",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/renderer/chunk/RenderChunkCache.getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/BlockState;"
            )
    )
    private BlockState getBlockState(RenderChunkCache chunkCache, BlockPos pos) {
        if (Baritone.settings().renderCachedChunks.value && !Minecraft.getInstance().isSingleplayer()) {
            Baritone baritone = (Baritone) BaritoneAPI.getProvider().getPrimaryBaritone();
            IPlayerContext ctx = baritone.getPlayerContext();
            if (ctx.player() != null && ctx.world() != null && baritone.bsi != null) {
                return baritone.bsi.get0(pos);
            }
        }

        return chunkCache.getBlockState(pos);
    }
}
