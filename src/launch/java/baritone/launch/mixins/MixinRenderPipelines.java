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

import baritone.utils.accessor.IRenderPipelines;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderPipelines.class)
public class MixinRenderPipelines implements IRenderPipelines {
    @Final @Shadow
    private static RenderPipeline.Snippet LINES_SNIPPET;

    @Final @Shadow
    private static RenderPipeline.Snippet MATRICES_FOG_SNIPPET;

    @Shadow
    private static RenderPipeline register(final RenderPipeline renderPipeline) { return null; }

    public RenderPipeline.Snippet getLinesSnippet() {
        return LINES_SNIPPET;
    }

    @Override
    public RenderPipeline.Snippet getMatricesFogSnippet() {
        return MATRICES_FOG_SNIPPET;
    }

    @Override
    public RenderPipeline baritone$registerPipeline(final RenderPipeline pipeline) {
        return register(pipeline);
    }
}
