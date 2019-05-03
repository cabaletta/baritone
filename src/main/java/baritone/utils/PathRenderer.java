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

import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.event.events.RenderEvent;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.*;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Helper;
import baritone.api.utils.interfaces.IGoalRenderPos;
import baritone.behavior.PathingBehavior;
import baritone.pathing.path.PathExecutor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntityBeaconRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author Brady
 * @since 8/9/2018
 */
public final class PathRenderer implements Helper {

    private static final Tessellator TESSELLATOR = Tessellator.getInstance();
    private static final BufferBuilder BUFFER = TESSELLATOR.getBuffer();

    private PathRenderer() {}

    public static void render(RenderEvent event, PathingBehavior behavior) {
        float partialTicks = event.getPartialTicks();
        Goal goal = behavior.getGoal();
        if (mc.currentScreen instanceof GuiClick) {
            ((GuiClick) mc.currentScreen).onRender();
        }

        int thisPlayerDimension = behavior.baritone.getPlayerContext().world().provider.getDimensionType().getId();
        int currentRenderViewDimension = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().world().provider.getDimensionType().getId();

        if (thisPlayerDimension != currentRenderViewDimension) {
            // this is a path for a bot in a different dimension, don't render it
            return;
        }

        Entity renderView = mc.getRenderViewEntity();

        if (renderView.world != BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().world()) {
            System.out.println("I have no idea what's going on");
            System.out.println("The primary baritone is in a different world than the render view entity");
            System.out.println("Not rendering the path");
            return;
        }

        if (goal != null && Baritone.settings().renderGoal.value) {
            drawDankLitGoalBox(renderView, goal, partialTicks, Baritone.settings().colorGoalBox.value);
        }

        if (!Baritone.settings().renderPath.value) {
            return;
        }
        PathExecutor current = behavior.getCurrent(); // this should prevent most race conditions?
        PathExecutor next = behavior.getNext(); // like, now it's not possible for current!=null to be true, then suddenly false because of another thread
        if (current != null && Baritone.settings().renderSelectionBoxes.value) {
            drawManySelectionBoxes(renderView, current.toBreak(), Baritone.settings().colorBlocksToBreak.value);
            drawManySelectionBoxes(renderView, current.toPlace(), Baritone.settings().colorBlocksToPlace.value);
            drawManySelectionBoxes(renderView, current.toWalkInto(), Baritone.settings().colorBlocksToWalkInto.value);
        }

        //drawManySelectionBoxes(player, Collections.singletonList(behavior.pathStart()), partialTicks, Color.WHITE);
        //long start = System.nanoTime();


        // Render the current path, if there is one
        if (current != null && current.getPath() != null) {
            int renderBegin = Math.max(current.getPosition() - 3, 0);
            drawPath(current.getPath(), renderBegin, renderView, partialTicks, Baritone.settings().colorCurrentPath.value, Baritone.settings().fadePath.value, 10, 20);
        }
        if (next != null && next.getPath() != null) {
            drawPath(next.getPath(), 0, renderView, partialTicks, Baritone.settings().colorNextPath.value, Baritone.settings().fadePath.value, 10, 20);
        }

        //long split = System.nanoTime();

        // If there is a path calculation currently running, render the path calculation process
        behavior.getInProgress().ifPresent(currentlyRunning -> {
            currentlyRunning.bestPathSoFar().ifPresent(p -> {
                drawPath(p, 0, renderView, partialTicks, Baritone.settings().colorBestPathSoFar.value, Baritone.settings().fadePath.value, 10, 20);
            });
            currentlyRunning.pathToMostRecentNodeConsidered().ifPresent(mr -> {

                drawPath(mr, 0, renderView, partialTicks, Baritone.settings().colorMostRecentConsidered.value, Baritone.settings().fadePath.value, 10, 20);
                drawManySelectionBoxes(renderView, Collections.singletonList(mr.getDest()), Baritone.settings().colorMostRecentConsidered.value);
            });
        });
        //long end = System.nanoTime();
        //System.out.println((end - split) + " " + (split - start));
        // if (end - start > 0) {
        //   System.out.println("Frame took " + (split - start) + " " + (end - split));
        //}
    }

    public static void drawPath(IPath path, int startIndex, Entity player, float partialTicks, Color color, boolean fadeOut, int fadeStart0, int fadeEnd0) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        GlStateManager.color(color.getColorComponents(null)[0], color.getColorComponents(null)[1], color.getColorComponents(null)[2], 0.4F);
        GlStateManager.glLineWidth(Baritone.settings().pathRenderLineWidthPixels.value);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        if (Baritone.settings().renderPathIgnoreDepth.value) {
            GlStateManager.disableDepth();
        }
        List<BetterBlockPos> positions = path.positions();
        int next;
        Tessellator tessellator = Tessellator.getInstance();
        int fadeStart = fadeStart0 + startIndex;
        int fadeEnd = fadeEnd0 + startIndex;
        for (int i = startIndex; i < positions.size() - 1; i = next) {
            BetterBlockPos start = positions.get(i);

            next = i + 1;
            BetterBlockPos end = positions.get(next);

            int dirX = end.x - start.x;
            int dirY = end.y - start.y;
            int dirZ = end.z - start.z;
            while (next + 1 < positions.size() && (!fadeOut || next + 1 < fadeStart) && (dirX == positions.get(next + 1).x - end.x && dirY == positions.get(next + 1).y - end.y && dirZ == positions.get(next + 1).z - end.z)) {
                next++;
                end = positions.get(next);
            }
            double x1 = start.x;
            double y1 = start.y;
            double z1 = start.z;
            double x2 = end.x;
            double y2 = end.y;
            double z2 = end.z;
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
            drawLine(x1, y1, z1, x2, y2, z2);
            tessellator.draw();
        }
        if (Baritone.settings().renderPathIgnoreDepth.value) {
            GlStateManager.enableDepth();
        }
        //GlStateManager.color(0.0f, 0.0f, 0.0f, 0.4f);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawLine(double bp1x, double bp1y, double bp1z, double bp2x, double bp2y, double bp2z) {
        double d0 = mc.getRenderManager().viewerPosX;
        double d1 = mc.getRenderManager().viewerPosY;
        double d2 = mc.getRenderManager().viewerPosZ;
        BUFFER.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        BUFFER.pos(bp1x + 0.5D - d0, bp1y + 0.5D - d1, bp1z + 0.5D - d2).endVertex();
        BUFFER.pos(bp2x + 0.5D - d0, bp2y + 0.5D - d1, bp2z + 0.5D - d2).endVertex();
        BUFFER.pos(bp2x + 0.5D - d0, bp2y + 0.53D - d1, bp2z + 0.5D - d2).endVertex();
        BUFFER.pos(bp1x + 0.5D - d0, bp1y + 0.53D - d1, bp1z + 0.5D - d2).endVertex();
        BUFFER.pos(bp1x + 0.5D - d0, bp1y + 0.5D - d1, bp1z + 0.5D - d2).endVertex();
    }

    public static void drawManySelectionBoxes(Entity player, Collection<BlockPos> positions, Color color) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(color.getColorComponents(null)[0], color.getColorComponents(null)[1], color.getColorComponents(null)[2], 0.4F);
        GlStateManager.glLineWidth(Baritone.settings().pathRenderLineWidthPixels.value);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        if (Baritone.settings().renderSelectionBoxesIgnoreDepth.value) {
            GlStateManager.disableDepth();
        }


        //BlockPos blockpos = movingObjectPositionIn.getBlockPos();
        BlockStateInterface bsi = new BlockStateInterface(BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext()); // TODO this assumes same dimension between primary baritone and render view? is this safe?
        positions.forEach(pos -> {
            IBlockState state = bsi.get0(pos);
            AxisAlignedBB toDraw;
            if (state.getBlock().equals(Blocks.AIR)) {
                toDraw = Blocks.DIRT.getDefaultState().getSelectedBoundingBox(player.world, pos);
            } else {
                toDraw = state.getSelectedBoundingBox(player.world, pos);
            }
            drawAABB(toDraw);
        });

        if (Baritone.settings().renderSelectionBoxesIgnoreDepth.value) {
            GlStateManager.enableDepth();
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawAABB(AxisAlignedBB aabb) {
        float expand = 0.002F;
        AxisAlignedBB toDraw = aabb.expand(expand, expand, expand).offset(-mc.getRenderManager().viewerPosX, -mc.getRenderManager().viewerPosY, -mc.getRenderManager().viewerPosZ);
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
    }

    public static void drawDankLitGoalBox(Entity player, Goal goal, float partialTicks, Color color) {
        double renderPosX = mc.getRenderManager().viewerPosX;
        double renderPosY = mc.getRenderManager().viewerPosY;
        double renderPosZ = mc.getRenderManager().viewerPosZ;
        double minX;
        double maxX;
        double minZ;
        double maxZ;
        double minY;
        double maxY;
        double y1;
        double y2;
        double y = MathHelper.cos((float) (((float) ((System.nanoTime() / 100000L) % 20000L)) / 20000F * Math.PI * 2));
        if (goal instanceof IGoalRenderPos) {
            BlockPos goalPos = ((IGoalRenderPos) goal).getGoalPos();
            minX = goalPos.getX() + 0.002 - renderPosX;
            maxX = goalPos.getX() + 1 - 0.002 - renderPosX;
            minZ = goalPos.getZ() + 0.002 - renderPosZ;
            maxZ = goalPos.getZ() + 1 - 0.002 - renderPosZ;
            if (goal instanceof GoalGetToBlock || goal instanceof GoalTwoBlocks) {
                y /= 2;
            }
            y1 = 1 + y + goalPos.getY() - renderPosY;
            y2 = 1 - y + goalPos.getY() - renderPosY;
            minY = goalPos.getY() - renderPosY;
            maxY = minY + 2;
            if (goal instanceof GoalGetToBlock || goal instanceof GoalTwoBlocks) {
                y1 -= 0.5;
                y2 -= 0.5;
                maxY--;
            }
        } else if (goal instanceof GoalXZ) {
            GoalXZ goalPos = (GoalXZ) goal;

            if (Baritone.settings().renderGoalXZBeacon.value) {
                glPushAttrib(GL_LIGHTING_BIT);

                mc.getTextureManager().bindTexture(TileEntityBeaconRenderer.TEXTURE_BEACON_BEAM);

                if (Baritone.settings().renderGoalIgnoreDepth.value) {
                    GlStateManager.disableDepth();
                }

                TileEntityBeaconRenderer.renderBeamSegment(
                        goalPos.getX() - renderPosX,
                        -renderPosY,
                        goalPos.getZ() - renderPosZ,
                        partialTicks,
                        1.0,
                        player.world.getTotalWorldTime(),
                        0,
                        256,
                        color.getColorComponents(null)
                );

                if (Baritone.settings().renderGoalIgnoreDepth.value) {
                    GlStateManager.enableDepth();
                }

                glPopAttrib();
                return;
            }

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
                drawDankLitGoalBox(player, g, partialTicks, color);
            }
            return;
        } else if (goal instanceof GoalYLevel) {
            GoalYLevel goalpos = (GoalYLevel) goal;
            minX = player.posX - Baritone.settings().yLevelBoxSize.value - renderPosX;
            minZ = player.posZ - Baritone.settings().yLevelBoxSize.value - renderPosZ;
            maxX = player.posX + Baritone.settings().yLevelBoxSize.value - renderPosX;
            maxZ = player.posZ + Baritone.settings().yLevelBoxSize.value - renderPosZ;
            minY = ((GoalYLevel) goal).level - renderPosY;
            maxY = minY + 2;
            y1 = 1 + y + goalpos.level - renderPosY;
            y2 = 1 - y + goalpos.level - renderPosY;
        } else {
            return;
        }

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(color.getColorComponents(null)[0], color.getColorComponents(null)[1], color.getColorComponents(null)[2], 0.6F);
        GlStateManager.glLineWidth(Baritone.settings().goalRenderLineWidthPixels.value);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        if (Baritone.settings().renderGoalIgnoreDepth.value) {
            GlStateManager.disableDepth();
        }

        renderHorizontalQuad(minX, maxX, minZ, maxZ, y1);
        renderHorizontalQuad(minX, maxX, minZ, maxZ, y2);

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

        if (Baritone.settings().renderGoalIgnoreDepth.value) {
            GlStateManager.enableDepth();
        }
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static void renderHorizontalQuad(double minX, double maxX, double minZ, double maxZ, double y) {
        if (y != 0) {
            BUFFER.begin(GL_LINE_LOOP, DefaultVertexFormats.POSITION);
            BUFFER.pos(minX, y, minZ).endVertex();
            BUFFER.pos(maxX, y, minZ).endVertex();
            BUFFER.pos(maxX, y, maxZ).endVertex();
            BUFFER.pos(minX, y, maxZ).endVertex();
            TESSELLATOR.draw();
        }
    }
}
