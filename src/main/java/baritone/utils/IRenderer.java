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

package baritone.utils;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import baritone.utils.accessor.IEntityRenderManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.minecraft.world.phys.AABB;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

public interface IRenderer {

    Tesselator tessellator = Tesselator.getInstance();
    BufferBuilder buffer = tessellator.getBuilder();
    IEntityRenderManager renderManager = (IEntityRenderManager) Minecraft.getInstance().getEntityRenderDispatcher();
    TextureManager textureManager = Minecraft.getInstance().getTextureManager();
    Settings settings = BaritoneAPI.getSettings();

    float[] color = new float[] {1.0F, 1.0F, 1.0F, 255.0F};

    static void glColor(Color color, float alpha) {
        float[] colorComponents = color.getColorComponents(null);
        IRenderer.color[0] = colorComponents[0];
        IRenderer.color[1] = colorComponents[1];
        IRenderer.color[2] = colorComponents[2];
        IRenderer.color[3] = alpha;
    }

    static void startLines(Color color, float alpha, float lineWidth, boolean ignoreDepth) {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        glColor(color, alpha);
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.disableTexture();
        RenderSystem.depthMask(false);

        if (ignoreDepth) {
            RenderSystem.disableDepthTest();
        }
        //TODO: check
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
    }

    static void startLines(Color color, float lineWidth, boolean ignoreDepth) {
        startLines(color, .4f, lineWidth, ignoreDepth);
    }

    static void endLines(boolean ignoredDepth) {
        tessellator.end();
        if (ignoredDepth) {
            RenderSystem.enableDepthTest();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    static void emitAABB(PoseStack stack, AABB aabb) {
        AABB toDraw = aabb.move(-renderManager.renderPosX(), -renderManager.renderPosY(), -renderManager.renderPosZ());

        Matrix4f matrix4f = stack.last().pose();
        // bottom
        buffer.vertex(matrix4f, (float) toDraw.minX, (float) toDraw.minY, (float) toDraw.minZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.maxX, (float) toDraw.minY, (float) toDraw.minZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.maxX, (float) toDraw.minY, (float) toDraw.minZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.maxX, (float) toDraw.minY, (float) toDraw.maxZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.maxX, (float) toDraw.minY, (float) toDraw.maxZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.minX, (float) toDraw.minY, (float) toDraw.maxZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.minX, (float) toDraw.minY, (float) toDraw.maxZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.minX, (float) toDraw.minY, (float) toDraw.minZ).color(color[0], color[1], color[2], color[3]).endVertex();
        // top
        buffer.vertex(matrix4f, (float) toDraw.minX, (float) toDraw.maxY, (float) toDraw.minZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.maxX, (float) toDraw.maxY, (float) toDraw.minZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.maxX, (float) toDraw.maxY, (float) toDraw.minZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.maxX, (float) toDraw.maxY, (float) toDraw.maxZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.maxX, (float) toDraw.maxY, (float) toDraw.maxZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.minX, (float) toDraw.maxY, (float) toDraw.maxZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.minX, (float) toDraw.maxY, (float) toDraw.maxZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.minX, (float) toDraw.maxY, (float) toDraw.minZ).color(color[0], color[1], color[2], color[3]).endVertex();
        // corners
        buffer.vertex(matrix4f, (float) toDraw.minX, (float) toDraw.minY, (float) toDraw.minZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.minX, (float) toDraw.maxY, (float) toDraw.minZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.maxX, (float) toDraw.minY, (float) toDraw.minZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.maxX, (float) toDraw.maxY, (float) toDraw.minZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.maxX, (float) toDraw.minY, (float) toDraw.maxZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.maxX, (float) toDraw.maxY, (float) toDraw.maxZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.minX, (float) toDraw.minY, (float) toDraw.maxZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(matrix4f, (float) toDraw.minX, (float) toDraw.maxY, (float) toDraw.maxZ).color(color[0], color[1], color[2], color[3]).endVertex();
    }

    static void emitAABB(PoseStack stack, AABB aabb, double expand) {
        emitAABB(stack, aabb.inflate(expand, expand, expand));
    }

    static void drawAABB(PoseStack stack, AABB aabb) {
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        emitAABB(stack, aabb);
        tessellator.end();
    }
}
