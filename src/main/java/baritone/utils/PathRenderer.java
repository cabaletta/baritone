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

package baritone.utils;

import baritone.Baritone;
import baritone.pathing.goals.Goal;
import baritone.pathing.goals.GoalComposite;
import baritone.pathing.goals.GoalTwoBlocks;
import baritone.pathing.goals.GoalXZ;
import baritone.pathing.path.IPath;
import baritone.utils.interfaces.IGoalRenderPos;
import baritone.utils.pathing.BetterBlockPos;
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
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author Brady
 * @since 8/9/2018 4:39 PM
 */
public final class PathRenderer implements Helper {
    
    private static final Tessellator TESSELLATOR = Tessellator.getInstance();
    private static final BufferBuilder BUFFER = TESSELLATOR.getBuffer();

    private PathRenderer() {}

    public static void drawPath(IPath path, int startIndex, EntityPlayerSP player, float partialTicks, Color color, boolean fadeOut, int fadeStart0, int fadeEnd0) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        GlStateManager.color(color.getColorComponents(null)[0], color.getColorComponents(null)[1], color.getColorComponents(null)[2], 0.4F);
        GlStateManager.glLineWidth(Baritone.settings().pathRenderLineWidthPixels.get());
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        List<BetterBlockPos> positions = path.positions();
        int next;
        Tessellator tessellator = Tessellator.getInstance();
        int fadeStart = fadeStart0 + startIndex;
        int fadeEnd = fadeEnd0 + startIndex;
        for (int i = startIndex; i < positions.size() - 1; i = next) {
            BlockPos start = positions.get(i);

            next = i + 1;
            BlockPos end = positions.get(next);

            BlockPos direction = Utils.diff(start, end);
            while (next + 1 < positions.size() && (!fadeOut || next + 1 < fadeStart) && direction.equals(Utils.diff(end, positions.get(next + 1)))) {
                next++;
                end = positions.get(next);
            }
            double x1 = start.getX();
            double y1 = start.getY();
            double z1 = start.getZ();
            double x2 = end.getX();
            double y2 = end.getY();
            double z2 = end.getZ();
            if (fadeOut) {

                float alpha;
                if (i <= fadeStart) {
                    alpha = 0.4F;
                } else {
                    if (i > fadeEnd) {
                        break;
                    }
                    alpha = 0.4F * (1.0F - (float) (i - fadeStart) / (float) (fadeEnd - fadeStart));
                }
                GlStateManager.color(color.getColorComponents(null)[0], color.getColorComponents(null)[1], color.getColorComponents(null)[2], alpha);
            }
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
        GlStateManager.glLineWidth(Baritone.settings().pathRenderLineWidthPixels.get());
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

    public static void drawLitDankGoalBox(EntityPlayer player, Goal goal, float partialTicks, Color color) {
        double renderPosX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTicks;
        double renderPosY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTicks;
        double renderPosZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTicks;

        double minX;
        double maxX;
        double minZ;
        double maxZ;
        double minY;
        double maxY;
        double y1;
        double y2;
        if (goal instanceof IGoalRenderPos) {
            BlockPos goalPos = ((IGoalRenderPos) goal).getGoalPos();
            minX = goalPos.getX() + 0.002 - renderPosX;
            maxX = goalPos.getX() + 1 - 0.002 - renderPosX;
            minZ = goalPos.getZ() + 0.002 - renderPosZ;
            maxZ = goalPos.getZ() + 1 - 0.002 - renderPosZ;
            double y = MathHelper.cos((float) (((float) ((System.nanoTime() / 100000L) % 20000L)) / 20000F * Math.PI * 2));
            if (goal instanceof GoalTwoBlocks) {
                y /= 2;
            }
            y1 = 1 + y + goalPos.getY() - renderPosY;
            y2 = 1 - y + goalPos.getY() - renderPosY;
            minY = goalPos.getY() - renderPosY;
            maxY = minY + 2;
            if (goal instanceof GoalTwoBlocks) {
                y1 -= 0.5;
                y2 -= 0.5;
                maxY--;
            }
        } else if (goal instanceof GoalXZ) {
            GoalXZ goalPos = (GoalXZ) goal;

            minX = goalPos.getX() + 0.002 - renderPosX;
            maxX = goalPos.getX() + 1 - 0.002 - renderPosX;
            minZ = goalPos.getZ() + 0.002 - renderPosZ;
            maxZ = goalPos.getZ() + 1 - 0.002 - renderPosZ;

            y1 = 0;
            y2 = 0;
            minY = 0 - renderPosY;
            maxY = 256 - renderPosY;
        } else if (goal instanceof GoalComposite) {
            for (Goal g : ((GoalComposite) goal).goals()) {
                drawLitDankGoalBox(player, g, partialTicks, color);
            }
            return;
        } else {
            return;
        }

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(color.getColorComponents(null)[0], color.getColorComponents(null)[1], color.getColorComponents(null)[2], 0.6F);
        GlStateManager.glLineWidth(Baritone.settings().goalRenderLineWidthPixels.get());
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        if (y1 != 0) {
            BUFFER.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION);
            BUFFER.pos(minX, y1, minZ).endVertex();
            BUFFER.pos(maxX, y1, minZ).endVertex();
            BUFFER.pos(maxX, y1, maxZ).endVertex();
            BUFFER.pos(minX, y1, maxZ).endVertex();
            BUFFER.pos(minX, y1, minZ).endVertex();
            TESSELLATOR.draw();
        }

        if (y2 != 0) {
            BUFFER.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION);
            BUFFER.pos(minX, y2, minZ).endVertex();
            BUFFER.pos(maxX, y2, minZ).endVertex();
            BUFFER.pos(maxX, y2, maxZ).endVertex();
            BUFFER.pos(minX, y2, maxZ).endVertex();
            BUFFER.pos(minX, y2, minZ).endVertex();
            TESSELLATOR.draw();
        }

        BUFFER.begin(GL_LINES, DefaultVertexFormats.POSITION);
        BUFFER.pos(minX, minY, minZ).endVertex();
        BUFFER.pos(minX, maxY, minZ).endVertex();
        BUFFER.pos(maxX, minY, minZ).endVertex();
        BUFFER.pos(maxX, maxY, minZ).endVertex();
        BUFFER.pos(maxX, minY, maxZ).endVertex();
        BUFFER.pos(maxX, maxY, maxZ).endVertex();
        BUFFER.pos(minX, minY, maxZ).endVertex();
        BUFFER.pos(minX, maxY, maxZ).endVertex();
        TESSELLATOR.draw();


        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
