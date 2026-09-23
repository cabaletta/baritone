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
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

public interface IRenderer {

    Tesselator tessellator = Tesselator.getInstance();
    IEntityRenderManager renderManager = (IEntityRenderManager) Minecraft.getInstance().getEntityRenderDispatcher();
    TextureManager textureManager = Minecraft.getInstance().getTextureManager();
    Settings settings = BaritoneAPI.getSettings();

    float[] color = new float[]{1.0F, 1.0F, 1.0F, 255.0F};

    static void glColor(Color color, float alpha) {
        float[] colorComponents = color.getColorComponents(null);
        IRenderer.color[0] = colorComponents[0];
        IRenderer.color[1] = colorComponents[1];
        IRenderer.color[2] = colorComponents[2];
        IRenderer.color[3] = alpha;
    }

    // see through, no depth writes, both sides. lines and quads want exactly the same things, and endLines undoes it for both
    static void startTranslucent(boolean ignoreDepth) {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        if (ignoreDepth) {
            RenderSystem.disableDepthTest();
        }
    }

    static BufferBuilder startLines(Color color, float alpha, float lineWidth, boolean ignoreDepth) {
        startTranslucent(ignoreDepth);
        glColor(color, alpha);
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.setShader(CoreShaders.RENDERTYPE_LINES);
        return tessellator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
    }

    static BufferBuilder startLines(Color color, float lineWidth, boolean ignoreDepth) {
        return startLines(color, .4f, lineWidth, ignoreDepth);
    }

    static void endLines(BufferBuilder bufferBuilder, boolean ignoredDepth) {
        MeshData meshData = bufferBuilder.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }

        if (ignoredDepth) {
            RenderSystem.enableDepthTest();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    // same tesselator as the lines, so finish your quads before you start your lines (or the other way around)
    static BufferBuilder startQuads(boolean ignoreDepth) {
        startTranslucent(ignoreDepth);
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);
        return tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    static void endQuads(BufferBuilder bufferBuilder, boolean ignoredDepth) {
        // the cleanup is identical, the shader got picked in startQuads
        endLines(bufferBuilder, ignoredDepth);
    }

    // uses the rgb from glColor but its own alpha, which is what makes gradients possible
    static void emitVertex(BufferBuilder bufferBuilder, PoseStack.Pose pose, double x, double y, double z, float alpha) {
        bufferBuilder.addVertex(pose, (float) x, (float) y, (float) z).setColor(color[0], color[1], color[2], alpha);
    }

    // a box with no lid and no floor, alpha goes from bottomAlpha to topAlpha on the way up. coordinates are already camera relative
    static void emitWalls(BufferBuilder bufferBuilder, PoseStack stack, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float bottomAlpha, float topAlpha) {
        PoseStack.Pose pose = stack.last();
        emitWall(bufferBuilder, pose, minX, minZ, maxX, minZ, minY, maxY, bottomAlpha, topAlpha);
        emitWall(bufferBuilder, pose, maxX, minZ, maxX, maxZ, minY, maxY, bottomAlpha, topAlpha);
        emitWall(bufferBuilder, pose, maxX, maxZ, minX, maxZ, minY, maxY, bottomAlpha, topAlpha);
        emitWall(bufferBuilder, pose, minX, maxZ, minX, minZ, minY, maxY, bottomAlpha, topAlpha);
    }

    static void emitWall(BufferBuilder bufferBuilder, PoseStack.Pose pose, double x1, double z1, double x2, double z2, double minY, double maxY, float bottomAlpha, float topAlpha) {
        emitVertex(bufferBuilder, pose, x1, minY, z1, bottomAlpha);
        emitVertex(bufferBuilder, pose, x2, minY, z2, bottomAlpha);
        emitVertex(bufferBuilder, pose, x2, maxY, z2, topAlpha);
        emitVertex(bufferBuilder, pose, x1, maxY, z1, topAlpha);
    }

    static void emitHorizontalQuad(BufferBuilder bufferBuilder, PoseStack stack, double minX, double minZ, double maxX, double maxZ, double y, float alpha) {
        PoseStack.Pose pose = stack.last();
        emitVertex(bufferBuilder, pose, minX, y, minZ, alpha);
        emitVertex(bufferBuilder, pose, maxX, y, minZ, alpha);
        emitVertex(bufferBuilder, pose, maxX, y, maxZ, alpha);
        emitVertex(bufferBuilder, pose, minX, y, maxZ, alpha);
    }

    static void emitFilledAABB(BufferBuilder bufferBuilder, PoseStack stack, AABB aabb, float alpha) {
        AABB toDraw = aabb.move(-renderManager.renderPosX(), -renderManager.renderPosY(), -renderManager.renderPosZ());
        emitWalls(bufferBuilder, stack, toDraw.minX, toDraw.minY, toDraw.minZ, toDraw.maxX, toDraw.maxY, toDraw.maxZ, alpha, alpha);
        emitHorizontalQuad(bufferBuilder, stack, toDraw.minX, toDraw.minZ, toDraw.maxX, toDraw.maxZ, toDraw.minY, alpha);
        emitHorizontalQuad(bufferBuilder, stack, toDraw.minX, toDraw.minZ, toDraw.maxX, toDraw.maxZ, toDraw.maxY, alpha);
    }

    static void emitLine(BufferBuilder bufferBuilder, PoseStack stack, double x1, double y1, double z1, double x2, double y2, double z2) {
        final double dx = x2 - x1;
        final double dy = y2 - y1;
        final double dz = z2 - z1;

        final double invMag = 1.0 / Math.sqrt(dx * dx + dy * dy + dz * dz);
        final float nx = (float) (dx * invMag);
        final float ny = (float) (dy * invMag);
        final float nz = (float) (dz * invMag);

        emitLine(bufferBuilder, stack, x1, y1, z1, x2, y2, z2, nx, ny, nz);
    }

    static void emitLine(BufferBuilder bufferBuilder, PoseStack stack,
                         double x1, double y1, double z1,
                         double x2, double y2, double z2,
                         double nx, double ny, double nz) {
        emitLine(bufferBuilder, stack,
                (float) x1, (float) y1, (float) z1,
                (float) x2, (float) y2, (float) z2,
                (float) nx, (float) ny, (float) nz
        );
    }

    static void emitLine(BufferBuilder bufferBuilder, PoseStack stack,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float nx, float ny, float nz) {
        PoseStack.Pose pose = stack.last();

        bufferBuilder.addVertex(pose, x1, y1, z1).setColor(color[0], color[1], color[2], color[3]).setNormal(pose, nx, ny, nz);
        bufferBuilder.addVertex(pose, x2, y2, z2).setColor(color[0], color[1], color[2], color[3]).setNormal(pose, nx, ny, nz);
    }

    static void emitAABB(BufferBuilder bufferBuilder, PoseStack stack, AABB aabb) {
        AABB toDraw = aabb.move(-renderManager.renderPosX(), -renderManager.renderPosY(), -renderManager.renderPosZ());

        // bottom
        emitLine(bufferBuilder, stack, toDraw.minX, toDraw.minY, toDraw.minZ, toDraw.maxX, toDraw.minY, toDraw.minZ, 1.0, 0.0, 0.0);
        emitLine(bufferBuilder, stack, toDraw.maxX, toDraw.minY, toDraw.minZ, toDraw.maxX, toDraw.minY, toDraw.maxZ, 0.0, 0.0, 1.0);
        emitLine(bufferBuilder, stack, toDraw.maxX, toDraw.minY, toDraw.maxZ, toDraw.minX, toDraw.minY, toDraw.maxZ, -1.0, 0.0, 0.0);
        emitLine(bufferBuilder, stack, toDraw.minX, toDraw.minY, toDraw.maxZ, toDraw.minX, toDraw.minY, toDraw.minZ, 0.0, 0.0, -1.0);
        // top
        emitLine(bufferBuilder, stack, toDraw.minX, toDraw.maxY, toDraw.minZ, toDraw.maxX, toDraw.maxY, toDraw.minZ, 1.0, 0.0, 0.0);
        emitLine(bufferBuilder, stack, toDraw.maxX, toDraw.maxY, toDraw.minZ, toDraw.maxX, toDraw.maxY, toDraw.maxZ, 0.0, 0.0, 1.0);
        emitLine(bufferBuilder, stack, toDraw.maxX, toDraw.maxY, toDraw.maxZ, toDraw.minX, toDraw.maxY, toDraw.maxZ, -1.0, 0.0, 0.0);
        emitLine(bufferBuilder, stack, toDraw.minX, toDraw.maxY, toDraw.maxZ, toDraw.minX, toDraw.maxY, toDraw.minZ, 0.0, 0.0, -1.0);
        // corners
        emitLine(bufferBuilder, stack, toDraw.minX, toDraw.minY, toDraw.minZ, toDraw.minX, toDraw.maxY, toDraw.minZ, 0.0, 1.0, 0.0);
        emitLine(bufferBuilder, stack, toDraw.maxX, toDraw.minY, toDraw.minZ, toDraw.maxX, toDraw.maxY, toDraw.minZ, 0.0, 1.0, 0.0);
        emitLine(bufferBuilder, stack, toDraw.maxX, toDraw.minY, toDraw.maxZ, toDraw.maxX, toDraw.maxY, toDraw.maxZ, 0.0, 1.0, 0.0);
        emitLine(bufferBuilder, stack, toDraw.minX, toDraw.minY, toDraw.maxZ, toDraw.minX, toDraw.maxY, toDraw.maxZ, 0.0, 1.0, 0.0);
    }

    static void emitAABB(BufferBuilder bufferBuilder, PoseStack stack, AABB aabb, double expand) {
        emitAABB(bufferBuilder, stack, aabb.inflate(expand, expand, expand));
    }

    static void emitLine(BufferBuilder bufferBuilder, PoseStack stack, Vec3 start, Vec3 end) {
        double vpX = renderManager.renderPosX();
        double vpY = renderManager.renderPosY();
        double vpZ = renderManager.renderPosZ();
        emitLine(bufferBuilder, stack, start.x - vpX, start.y - vpY, start.z - vpZ, end.x - vpX, end.y - vpY, end.z - vpZ);
    }

}
