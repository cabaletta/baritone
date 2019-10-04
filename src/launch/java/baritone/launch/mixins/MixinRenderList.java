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
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static org.lwjgl.opengl.GL11.*;

@Mixin(RenderList.class)
public class MixinRenderList {

    @Redirect( // avoid creating CallbackInfo at all costs; this is called 40k times per second
            method = "renderChunkLayer",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/renderer/GlStateManager.popMatrix()V"
            )
    )
    private void popMatrix() {
        if (Baritone.settings().renderCachedChunks.value && !Minecraft.getMinecraft().isSingleplayer()) {
            // reset the blend func to normal (not dependent on constant alpha)
            GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        }
        GlStateManager.popMatrix();
    }
}
