/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot.utils;

import baritone.bot.pathing.path.IPath;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import java.awt.*;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author Brady
 * @since 8/9/2018 4:39 PM
 */
public final class PathRenderer implements Helper {

    private PathRenderer() {}

    private static final Tessellator TESSELLATOR = Tessellator.getInstance();
    private static final BufferBuilder BUFFER = TESSELLATOR.getBuffer();

    public static void drawPath(IPath path, int startIndex, EntityPlayerSP player, float partialTicks, Color color) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        GlStateManager.color(color.getColorComponents(null)[0], color.getColorComponents(null)[1], color.getColorComponents(null)[2], 0.4F);
        GlStateManager.glLineWidth(3.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        List<BlockPos> positions = path.positions();
        int next;
        Tessellator tessellator = Tessellator.getInstance();
        for (int i = startIndex; i < positions.size() - 1; i = next) {
            BlockPos start = positions.get(i);

            next = i + 1;
            BlockPos end = positions.get(next);
            BlockPos direction = Utils.diff(start, end);
            while (next + 1 < positions.size() && direction.equals(Utils.diff(end, positions.get(next + 1)))) {
                next++;
                end = positions.get(next);
            }
            double x1 = start.getX();
            double y1 = start.getY();
            double z1 = start.getZ();
            double x2 = end.getX();
            double y2 = end.getY();
            double z2 = end.getZ();
            drawLine(player, x1, y1, z1, x2, y2, z2, partialTicks);
            tessellator.draw();
        }
        //GlStateManager.color(0.0f, 0.0f, 0.0f, 0.4f);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawLine(EntityPlayer player, double bp1x, double bp1y, double bp1z, double bp2x, double bp2y, double bp2z, float partialTicks) {
        double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTicks;
        double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTicks;
        double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTicks;
        BUFFER.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        BUFFER.pos(bp1x + 0.5D - d0, bp1y + 0.5D - d1, bp1z + 0.5D - d2).endVertex();
        BUFFER.pos(bp2x + 0.5D - d0, bp2y + 0.5D - d1, bp2z + 0.5D - d2).endVertex();
        BUFFER.pos(bp2x + 0.5D - d0, bp2y + 0.53D - d1, bp2z + 0.5D - d2).endVertex();
        BUFFER.pos(bp1x + 0.5D - d0, bp1y + 0.53D - d1, bp1z + 0.5D - d2).endVertex();
        BUFFER.pos(bp1x + 0.5D - d0, bp1y + 0.5D - d1, bp1z + 0.5D - d2).endVertex();
    }

    public static void drawManySelectionBoxes(EntityPlayer player, Collection<BlockPos> positions, float partialTicks, Color color) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(color.getColorComponents(null)[0], color.getColorComponents(null)[1], color.getColorComponents(null)[2], 0.4F);
        GlStateManager.glLineWidth(5.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        float expand = 0.002F;
        //BlockPos blockpos = movingObjectPositionIn.getBlockPos();

        double renderPosX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTicks;
        double renderPosY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTicks;
        double renderPosZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTicks;
        positions.forEach(pos -> {
            IBlockState state = BlockStateInterface.get(pos);
            AxisAlignedBB toDraw;
            if (state.getBlock().equals(Blocks.AIR)) {
                toDraw = Blocks.DIRT.getDefaultState().getSelectedBoundingBox(Minecraft.getMinecraft().world, pos);
            } else {
                toDraw = state.getSelectedBoundingBox(Minecraft.getMinecraft().world, pos);
            }
            toDraw = toDraw.expand(expand, expand, expand).offset(-renderPosX, -renderPosY, -renderPosZ);
            BUFFER.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION);
            BUFFER.pos(toDraw.minX, toDraw.minY, toDraw.minZ).endVertex();
            BUFFER.pos(toDraw.maxX, toDraw.minY, toDraw.minZ).endVertex();
            BUFFER.pos(toDraw.maxX, toDraw.minY, toDraw.maxZ).endVertex();
            BUFFER.pos(toDraw.minX, toDraw.minY, toDraw.maxZ).endVertex();
            BUFFER.pos(toDraw.minX, toDraw.minY, toDraw.minZ).endVertex();
            TESSELLATOR.draw();
            BUFFER.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION);
            BUFFER.pos(toDraw.minX, toDraw.maxY, toDraw.minZ).endVertex();
            BUFFER.pos(toDraw.maxX, toDraw.maxY, toDraw.minZ).endVertex();
            BUFFER.pos(toDraw.maxX, toDraw.maxY, toDraw.maxZ).endVertex();
            BUFFER.pos(toDraw.minX, toDraw.maxY, toDraw.maxZ).endVertex();
            BUFFER.pos(toDraw.minX, toDraw.maxY, toDraw.minZ).endVertex();
            TESSELLATOR.draw();
            BUFFER.begin(GL_LINES, DefaultVertexFormats.POSITION);
            BUFFER.pos(toDraw.minX, toDraw.minY, toDraw.minZ).endVertex();
            BUFFER.pos(toDraw.minX, toDraw.maxY, toDraw.minZ).endVertex();
            BUFFER.pos(toDraw.maxX, toDraw.minY, toDraw.minZ).endVertex();
            BUFFER.pos(toDraw.maxX, toDraw.maxY, toDraw.minZ).endVertex();
            BUFFER.pos(toDraw.maxX, toDraw.minY, toDraw.maxZ).endVertex();
            BUFFER.pos(toDraw.maxX, toDraw.maxY, toDraw.maxZ).endVertex();
            BUFFER.pos(toDraw.minX, toDraw.minY, toDraw.maxZ).endVertex();
            BUFFER.pos(toDraw.minX, toDraw.maxY, toDraw.maxZ).endVertex();
            TESSELLATOR.draw();
        });

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
