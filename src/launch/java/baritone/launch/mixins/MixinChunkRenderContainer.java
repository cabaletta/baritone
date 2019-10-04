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
import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL14;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static org.lwjgl.opengl.GL11.*;

@Mixin(ChunkRenderContainer.class)
public class MixinChunkRenderContainer {

    @Redirect( // avoid creating CallbackInfo at all costs; this is called 40k times per second
            method = "preRenderChunk",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/renderer/chunk/RenderChunk.getPosition()Lnet/minecraft/util/math/BlockPos;"
            )
    )
    private BlockPos getPosition(RenderChunk renderChunkIn) {
        if (Baritone.settings().renderCachedChunks.value && !Minecraft.getMinecraft().isSingleplayer() && Minecraft.getMinecraft().world.getChunk(renderChunkIn.getPosition()).isEmpty()) {
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GL14.glBlendColor(0, 0, 0, Baritone.settings().cachedChunksOpacity.value);
            GlStateManager.tryBlendFuncSeparate(GL_CONSTANT_ALPHA, GL_ONE_MINUS_CONSTANT_ALPHA, GL_ONE, GL_ZERO);
        }
        return renderChunkIn.getPosition();
    }
}
