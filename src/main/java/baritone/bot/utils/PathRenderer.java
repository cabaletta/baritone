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
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author Brady
 * @since 8/9/2018 4:39 PM
 */
public final class PathRenderer implements Helper {

    private PathRenderer() {}

    public static void drawPath(IPath path, int startIndex, EntityPlayerSP player, float partialTicks, Color color) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(color.getColorComponents(null)[0], color.getColorComponents(null)[1], color.getColorComponents(null)[2], 0.4F);
        GL11.glLineWidth(3.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        List<BlockPos> positions = path.positions();
        int next;
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
        }
        // GlStateManager.color(0.0f, 0.0f, 0.0f, 0.4f);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawLine(EntityPlayer player, double bp1x, double bp1y, double bp1z, double bp2x, double bp2y, double bp2z, float partialTicks) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTicks;
        double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTicks;
        double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTicks;
        buffer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        buffer.pos(bp1x + 0.5D - d0, bp1y + 0.5D - d1, bp1z + 0.5D - d2).endVertex();
        buffer.pos(bp2x + 0.5D - d0, bp2y + 0.5D - d1, bp2z + 0.5D - d2).endVertex();
        buffer.pos(bp2x + 0.5D - d0, bp2y + 0.53D - d1, bp2z + 0.5D - d2).endVertex();
        buffer.pos(bp1x + 0.5D - d0, bp1y + 0.53D - d1, bp1z + 0.5D - d2).endVertex();
        buffer.pos(bp1x + 0.5D - d0, bp1y + 0.5D - d1, bp1z + 0.5D - d2).endVertex();
        tessellator.draw();
    }

    public static void drawSelectionBox(EntityPlayer player, BlockPos blockpos, float partialTicks, Color color) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        GlStateManager.color(color.getColorComponents(null)[0], color.getColorComponents(null)[1], color.getColorComponents(null)[2], 0.4F);
        GL11.glLineWidth(5.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        float f = 0.002F;
        // BlockPos blockpos = movingObjectPositionIn.getBlockPos();
        IBlockState state = BlockStateInterface.get(blockpos);
        if (state.getBlock().equals(Blocks.AIR)) {
            state = Blocks.DIRT.getDefaultState();
        }
        // block.setBlockBoundsBasedOnState(Minecraft.getMinecraft().world, blockpos);
        double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTicks;
        double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTicks;
        double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTicks;
        AxisAlignedBB toDraw = state.getSelectedBoundingBox(mc.world, blockpos).expand(f, f, f).offset(-d0, -d1, -d2);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        buffer.pos(toDraw.minX, toDraw.minY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.minY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.minY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.minX, toDraw.minY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.minX, toDraw.minY, toDraw.minZ).endVertex();
        tessellator.draw();
        buffer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        buffer.pos(toDraw.minX, toDraw.maxY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.maxY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.maxY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.minX, toDraw.maxY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.minX, toDraw.maxY, toDraw.minZ).endVertex();
        tessellator.draw();
        buffer.begin(GL_LINES, DefaultVertexFormats.POSITION);
        buffer.pos(toDraw.minX, toDraw.minY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.minX, toDraw.maxY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.minY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.maxY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.minY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.maxY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.minX, toDraw.minY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.minX, toDraw.maxY, toDraw.maxZ).endVertex();
        tessellator.draw();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
