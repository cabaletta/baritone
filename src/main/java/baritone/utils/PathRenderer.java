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
import baritone.api.event.events.RenderEvent;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.*;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.interfaces.IGoalRenderPos;
import baritone.behavior.PathingBehavior;
import baritone.pathing.path.PathExecutor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityBeaconRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Brady
 * @since 8/9/2018
 */
public final class PathRenderer implements IRenderer {

    private PathRenderer() {}

    public static void render(RenderEvent event, PathingBehavior behavior) {
        final IPlayerContext ctx = behavior.ctx;
        if (ctx.world() == null) {
            return;
        }
        if (ctx.minecraft().currentScreen instanceof GuiClick && behavior.baritone == BaritoneAPI.getProvider().getPrimaryBaritone()) {
            ((GuiClick) ctx.minecraft().currentScreen).onRender();
        }

        final float partialTicks = event.getPartialTicks();
        final Goal goal = behavior.getGoal();

        final int thisPlayerDimension = ctx.world().provider.getDimensionType().getId();
        final int currentRenderViewDimension = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().world().provider.getDimensionType().getId();

        if (thisPlayerDimension != currentRenderViewDimension) {
            // this is a path for a bot in a different dimension, don't render it
            return;
        }

        if (goal != null && settings.renderGoal.value) {
            drawGoal(ctx.player(), goal, partialTicks, settings.colorGoalBox.value);
        }

        if (!settings.renderPath.value) {
            return;
        }

        PathExecutor current = behavior.getCurrent(); // this should prevent most race conditions?
        PathExecutor next = behavior.getNext(); // like, now it's not possible for current!=null to be true, then suddenly false because of another thread
        if (current != null && settings.renderSelectionBoxes.value) {
            drawManySelectionBoxes(ctx.player(), current.toBreak(), settings.colorBlocksToBreak.value);
            drawManySelectionBoxes(ctx.player(), current.toPlace(), settings.colorBlocksToPlace.value);
            drawManySelectionBoxes(ctx.player(), current.toWalkInto(), settings.colorBlocksToWalkInto.value);
        }

        //drawManySelectionBoxes(player, Collections.singletonList(behavior.pathStart()), partialTicks, Color.WHITE);

        // Render the current path, if there is one
        if (current != null && current.getPath() != null) {
            int renderBegin = Math.max(current.getPosition() - 3, 0);
            drawPath(current.getPath(), renderBegin, settings.colorCurrentPath.value, settings.fadePath.value, 10, 20);
        }

        if (next != null && next.getPath() != null) {
            drawPath(next.getPath(), 0, settings.colorNextPath.value, settings.fadePath.value, 10, 20);
        }

        // If there is a path calculation currently running, render the path calculation process
        behavior.getInProgress().ifPresent(currentlyRunning -> {
            currentlyRunning.bestPathSoFar().ifPresent(p -> {
                drawPath(p, 0, settings.colorBestPathSoFar.value, settings.fadePath.value, 10, 20);
            });

            currentlyRunning.pathToMostRecentNodeConsidered().ifPresent(mr -> {
                drawPath(mr, 0, settings.colorMostRecentConsidered.value, settings.fadePath.value, 10, 20);
                drawManySelectionBoxes(ctx.player(), Collections.singletonList(mr.getDest()), settings.colorMostRecentConsidered.value);
            });
        });
    }

    private static void drawPath(IPath path, int startIndex, Color color, boolean fadeOut, int fadeStart0, int fadeEnd0) {
        IRenderer.startLines(color, settings.pathRenderLineWidthPixels.value, settings.renderPathIgnoreDepth.value);

        int fadeStart = fadeStart0 + startIndex;
        int fadeEnd = fadeEnd0 + startIndex;

        List<BetterBlockPos> positions = path.positions();
        for (int i = startIndex, next; i < positions.size() - 1; i = next) {
            BetterBlockPos start = positions.get(i);
            BetterBlockPos end = positions.get(next = i + 1);

            int dirX = end.x - start.x;
            int dirY = end.y - start.y;
            int dirZ = end.z - start.z;

            while (next + 1 < positions.size() && (!fadeOut || next + 1 < fadeStart) &&
                    (dirX == positions.get(next + 1).x - end.x &&
                            dirY == positions.get(next + 1).y - end.y &&
                            dirZ == positions.get(next + 1).z - end.z)) {
                end = positions.get(++next);
            }

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

                IRenderer.glColor(color, alpha);
            }

            emitLine(start.x, start.y, start.z, end.x, end.y, end.z);
        }

        IRenderer.endLines(settings.renderPathIgnoreDepth.value);
    }

    private static void emitLine(double x1, double y1, double z1, double x2, double y2, double z2) {
        double vpX = renderManager.viewerPosX;
        double vpY = renderManager.viewerPosY;
        double vpZ = renderManager.viewerPosZ;
        boolean renderPathAsFrickinThingy = !settings.renderPathAsLine.value;

        buffer.pos(x1 + 0.5D - vpX, y1 + 0.5D - vpY, z1 + 0.5D - vpZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.pos(x2 + 0.5D - vpX, y2 + 0.5D - vpY, z2 + 0.5D - vpZ).color(color[0], color[1], color[2], color[3]).endVertex();

        if (renderPathAsFrickinThingy) {
            buffer.pos(x2 + 0.5D - vpX, y2 + 0.5D - vpY, z2 + 0.5D - vpZ).color(color[0], color[1], color[2], color[3]).endVertex();
            buffer.pos(x2 + 0.5D - vpX, y2 + 0.53D - vpY, z2 + 0.5D - vpZ).color(color[0], color[1], color[2], color[3]).endVertex();

            buffer.pos(x2 + 0.5D - vpX, y2 + 0.53D - vpY, z2 + 0.5D - vpZ).color(color[0], color[1], color[2], color[3]).endVertex();
            buffer.pos(x1 + 0.5D - vpX, y1 + 0.53D - vpY, z1 + 0.5D - vpZ).color(color[0], color[1], color[2], color[3]).endVertex();

            buffer.pos(x1 + 0.5D - vpX, y1 + 0.53D - vpY, z1 + 0.5D - vpZ).color(color[0], color[1], color[2], color[3]).endVertex();
            buffer.pos(x1 + 0.5D - vpX, y1 + 0.5D - vpY, z1 + 0.5D - vpZ).color(color[0], color[1], color[2], color[3]).endVertex();
        }
    }

    public static void drawManySelectionBoxes(Entity player, Collection<BlockPos> positions, Color color) {
        IRenderer.startLines(color, settings.pathRenderLineWidthPixels.value, settings.renderSelectionBoxesIgnoreDepth.value);

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

            IRenderer.emitAABB(toDraw, .002D);
        });

        IRenderer.endLines(settings.renderSelectionBoxesIgnoreDepth.value);
    }

    private static void drawGoal(Entity player, Goal goal, float partialTicks, Color color) {
        drawGoal(player, goal, partialTicks, color, true);
    }

    private static void drawGoal(Entity player, Goal goal, float partialTicks, Color color, boolean setupRender) {
        double renderPosX = renderManager.viewerPosX;
        double renderPosY = renderManager.viewerPosY;
        double renderPosZ = renderManager.viewerPosZ;
        double minX, maxX;
        double minZ, maxZ;
        double minY, maxY;
        double y, y1, y2;
        if (!settings.renderGoalAnimated.value) {
            // y = 1 causes rendering issues when the player is at the same y as the top of a block for some reason
            y = 0.999F;
        } else {
            y = MathHelper.cos((float) (((float) ((System.nanoTime() / 100000L) % 20000L)) / 20000F * Math.PI * 2));
        }
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
            drawDankLitGoalBox(color, minX, maxX, minZ, maxZ, minY, maxY, y1, y2, setupRender);
        } else if (goal instanceof GoalXZ) {
            GoalXZ goalPos = (GoalXZ) goal;

            if (settings.renderGoalXZBeacon.value) {
                textureManager.bindTexture(TileEntityBeaconRenderer.TEXTURE_BEACON_BEAM);

                if (settings.renderGoalIgnoreDepth.value) {
                    GlStateManager.disableDepth();
                }

                TileEntityBeaconRenderer.renderBeamSegment(
                        goalPos.getX() - renderPosX,
                        -renderPosY,
                        goalPos.getZ() - renderPosZ,
                        settings.renderGoalAnimated.value ? partialTicks : 0,
                        1.0,
                        settings.renderGoalAnimated.value ? player.world.getTotalWorldTime() : 0,
                        0,
                        256,
                        color.getColorComponents(null)
                );

                if (settings.renderGoalIgnoreDepth.value) {
                    GlStateManager.enableDepth();
                }
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
            drawDankLitGoalBox(color, minX, maxX, minZ, maxZ, minY, maxY, y1, y2, setupRender);
        } else if (goal instanceof GoalComposite) {
            // Simple way to determine if goals can be batched, without having some sort of GoalRenderer
            boolean batch = Arrays.stream(((GoalComposite) goal).goals()).allMatch(IGoalRenderPos.class::isInstance);

            if (batch) {
                IRenderer.startLines(color, settings.goalRenderLineWidthPixels.value, settings.renderGoalIgnoreDepth.value);
            }
            for (Goal g : ((GoalComposite) goal).goals()) {
                drawGoal(player, g, partialTicks, color, !batch);
            }
            if (batch) {
                IRenderer.endLines(settings.renderGoalIgnoreDepth.value);
            }
        } else if (goal instanceof GoalInverted) {
            drawGoal(player, ((GoalInverted) goal).origin, partialTicks, settings.colorInvertedGoalBox.value);
        } else if (goal instanceof GoalYLevel) {
            GoalYLevel goalpos = (GoalYLevel) goal;
            minX = player.posX - settings.yLevelBoxSize.value - renderPosX;
            minZ = player.posZ - settings.yLevelBoxSize.value - renderPosZ;
            maxX = player.posX + settings.yLevelBoxSize.value - renderPosX;
            maxZ = player.posZ + settings.yLevelBoxSize.value - renderPosZ;
            minY = ((GoalYLevel) goal).level - renderPosY;
            maxY = minY + 2;
            y1 = 1 + y + goalpos.level - renderPosY;
            y2 = 1 - y + goalpos.level - renderPosY;
            drawDankLitGoalBox(color, minX, maxX, minZ, maxZ, minY, maxY, y1, y2, setupRender);
        }
    }

    private static void drawDankLitGoalBox(Color colorIn, double minX, double maxX, double minZ, double maxZ, double minY, double maxY, double y1, double y2, boolean setupRender) {
        if (setupRender) {
            IRenderer.startLines(colorIn, settings.goalRenderLineWidthPixels.value, settings.renderGoalIgnoreDepth.value);
        }

        renderHorizontalQuad(minX, maxX, minZ, maxZ, y1);
        renderHorizontalQuad(minX, maxX, minZ, maxZ, y2);

        buffer.pos(minX, minY, minZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.pos(minX, maxY, minZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.pos(maxX, minY, minZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.pos(maxX, maxY, minZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.pos(maxX, minY, maxZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.pos(minX, minY, maxZ).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.pos(minX, maxY, maxZ).color(color[0], color[1], color[2], color[3]).endVertex();

        if (setupRender) {
            IRenderer.endLines(settings.renderGoalIgnoreDepth.value);
        }
    }

    private static void renderHorizontalQuad(double minX, double maxX, double minZ, double maxZ, double y) {
        if (y != 0) {
            buffer.pos(minX, y, minZ).color(color[0], color[1], color[2], color[3]).endVertex();
            buffer.pos(maxX, y, minZ).color(color[0], color[1], color[2], color[3]).endVertex();

            buffer.pos(maxX, y, minZ).color(color[0], color[1], color[2], color[3]).endVertex();
            buffer.pos(maxX, y, maxZ).color(color[0], color[1], color[2], color[3]).endVertex();

            buffer.pos(maxX, y, maxZ).color(color[0], color[1], color[2], color[3]).endVertex();
            buffer.pos(minX, y, maxZ).color(color[0], color[1], color[2], color[3]).endVertex();

            buffer.pos(minX, y, maxZ).color(color[0], color[1], color[2], color[3]).endVertex();
            buffer.pos(minX, y, minZ).color(color[0], color[1], color[2], color[3]).endVertex();
        }
    }
}
