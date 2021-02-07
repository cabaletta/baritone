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
import baritone.api.utils.Helper;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.AxisAlignedBB;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

public interface IRenderer {

    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();
    RenderManager renderManager = Helper.mc.getRenderManager();
    Settings settings = BaritoneAPI.getSettings();

    static void glColor(Color color, float alpha) {
        float[] colorComponents = color.getColorComponents(null);
        GlStateManager.color(colorComponents[0], colorComponents[1], colorComponents[2], alpha);
    }

    static void startLines(Color color, float alpha, float lineWidth, boolean ignoreDepth) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        glColor(color, alpha);
        GlStateManager.glLineWidth(lineWidth);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        if (ignoreDepth) {
            GlStateManager.disableDepth();
        }
    }

    static void startLines(Color color, float lineWidth, boolean ignoreDepth) {
        startLines(color, .4f, lineWidth, ignoreDepth);
    }

    static void endLines(boolean ignoredDepth) {
        if (ignoredDepth) {
            GlStateManager.enableDepth();
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    static void drawAABB(AxisAlignedBB aabb) {
        AxisAlignedBB toDraw = aabb.offset(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ);

        buffer.begin(GL_LINES, DefaultVertexFormats.POSITION);
        // bottom
        buffer.pos(toDraw.minX, toDraw.minY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.minY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.minY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.minY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.minY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.minX, toDraw.minY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.minX, toDraw.minY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.minX, toDraw.minY, toDraw.minZ).endVertex();
        // top
        buffer.pos(toDraw.minX, toDraw.maxY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.maxY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.maxY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.maxY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.maxY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.minX, toDraw.maxY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.minX, toDraw.maxY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.minX, toDraw.maxY, toDraw.minZ).endVertex();
        // corners
        buffer.pos(toDraw.minX, toDraw.minY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.minX, toDraw.maxY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.minY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.maxY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.minY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.maxY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.minX, toDraw.minY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.minX, toDraw.maxY, toDraw.maxZ).endVertex();
        tessellator.draw();
    }

    static void drawAABB(AxisAlignedBB aabb, double expand) {
        drawAABB(aabb.grow(expand, expand, expand));
    }
}
