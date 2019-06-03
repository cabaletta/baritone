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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
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
                    target = "net/minecraft/world/ChunkCache.isEmpty()Z"
            )
    )
    private boolean isEmpty(ChunkCache chunkCache) {
        if (!chunkCache.isEmpty()) {
            return false;
        }
        if (Baritone.settings().renderCachedChunks.value && !Minecraft.getMinecraft().isSingleplayer()) {
            Baritone baritone = (Baritone) BaritoneAPI.getProvider().getPrimaryBaritone();
            IPlayerContext ctx = baritone.getPlayerContext();
            if (ctx.player() != null && ctx.world() != null && baritone.bsi != null) {
                BlockPos position = ((RenderChunk) (Object) this).getPosition();
                // RenderChunk extends from -1,-1,-1 to +16,+16,+16
                // then the constructor of ChunkCache extends it one more (presumably to get things like the connected status of fences? idk)
                // so if ANY of the adjacent chunks are loaded, we are unempty
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (baritone.bsi.isLoaded(16 * dx + position.getX(), 16 * dz + position.getZ())) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    @Redirect(
            method = "rebuildChunk",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/world/ChunkCache.getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"
            )
    )
    private IBlockState getBlockState(ChunkCache chunkCache, BlockPos pos) {
        if (Baritone.settings().renderCachedChunks.value && !Minecraft.getMinecraft().isSingleplayer()) {
            Baritone baritone = (Baritone) BaritoneAPI.getProvider().getPrimaryBaritone();
            IPlayerContext ctx = baritone.getPlayerContext();
            if (ctx.player() != null && ctx.world() != null && baritone.bsi != null) {
                return baritone.bsi.get0(pos);
            }
        }

        return chunkCache.getBlockState(pos);
    }
}
