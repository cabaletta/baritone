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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkRenderWorker;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkRenderWorker.class)
public abstract class MixinChunkRenderWorker {

    @Shadow
    protected abstract boolean isChunkExisting(BlockPos pos, World worldIn);

    @Redirect(
            method = "processTask",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/renderer/chunk/ChunkRenderWorker.isChunkExisting(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/World;)Z"
            )
    )
    private boolean isChunkExisting(ChunkRenderWorker worker, BlockPos pos, World world) {
        if (Baritone.settings().renderCachedChunks.value && !Minecraft.getMinecraft().isSingleplayer()) {
            Baritone baritone = (Baritone) BaritoneAPI.getProvider().getPrimaryBaritone();
            IPlayerContext ctx = baritone.getPlayerContext();
            if (ctx.player() != null && ctx.world() != null && baritone.bsi != null) {
                return baritone.bsi.isLoaded(pos.getX(), pos.getZ()) || this.isChunkExisting(pos, world);
            }
        }

        return this.isChunkExisting(pos, world);
    }
}

