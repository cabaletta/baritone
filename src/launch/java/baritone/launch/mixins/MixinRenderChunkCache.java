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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.RenderChunkCache;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderChunkCache.class)
public class MixinRenderChunkCache {
    @Redirect(
            method = "generateCache",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/world/chunk/Chunk.isEmptyBetween(II)Z"
            )
    )
    private static boolean isEmpty(Chunk chunk, int yStart, int yEnd) {
        if (!chunk.isEmptyBetween(yStart, yEnd)) {
            return false;
        }
        if (chunk.isEmpty() && Baritone.settings().renderCachedChunks.value && Minecraft.getInstance().getIntegratedServer() == null) {
            return false;
        }
        return true;
    }
}
