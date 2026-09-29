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
import baritone.api.pathing.calc.IPathFinder;
import baritone.api.pathing.goals.*;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.interfaces.IGoalRenderPos;
import baritone.behavior.PathingBehavior;
import baritone.pathing.path.PathExecutor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Brady
 * @since 8/9/2018
 */
public final class PathRenderer implements IRenderer {

    private static final ResourceLocation TEXTURE_BEACON_BEAM = ResourceLocation.parse("textures/entity/beacon_beam.png");

    private static final float BOX_FILL_ALPHA = 0.13F;
    private static final float GOAL_FILL_ALPHA = 0.32F;
    private static final float OUTLINE_ALPHA = 0.7F;

    // it has to remember last frame to be smooth about this one, and every bot has its own search going
    private static final Map<PathingBehavior, SearchGlow> SEARCH_GLOW = new WeakHashMap<>();

    private PathRenderer() {}

    public static double posX() {
        return renderManager.renderPosX();
    }

    public static double posY() {
        return renderManager.renderPosY();
    }

    public static double posZ() {
        return renderManager.renderPosZ();
    }

    public static void render(RenderEvent event, PathingBehavior behavior) {
        final IPlayerContext ctx = behavior.ctx;
        if (ctx.world() == null) {
            return;
        }
        if (ctx.minecraft().screen instanceof GuiClick) {
            ((GuiClick) ctx.minecraft().screen).onRender(event.getModelViewStack(), event.getProjectionMatrix());
        }

        final float partialTicks = event.getPartialTicks();
        final Goal goal = behavior.getGoal();

        final DimensionType thisPlayerDimension = ctx.world().dimensionType();
        final DimensionType currentRenderViewDimension = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().world().dimensionType();

        if (thisPlayerDimension != currentRenderViewDimension) {
            // this is a path for a bot in a different dimension, don't render it
            return;
        }

        if (goal != null && settings.renderGoal.value) {
            drawGoal(event.getModelViewStack(), ctx, goal, partialTicks, settings.colorGoalBox.value);
        }

        if (!settings.renderPath.value) {
            return;
        }

        PathExecutor current = behavior.getCurrent(); // this should prevent most race conditions?
        PathExecutor next = behavior.getNext(); // like, now it's not possible for current!=null to be true, then suddenly false because of another thread
        if (current != null && settings.renderSelectionBoxes.value) {
            drawManySelectionBoxes(event.getModelViewStack(), ctx.player(), current.toBreak(), settings.colorBlocksToBreak.value);
            drawManySelectionBoxes(event.getModelViewStack(), ctx.player(), current.toPlace(), settings.colorBlocksToPlace.value);
            drawManySelectionBoxes(event.getModelViewStack(), ctx.player(), current.toWalkInto(), settings.colorBlocksToWalkInto.value);
        }

        //drawManySelectionBoxes(player, Collections.singletonList(behavior.pathStart()), partialTicks, Color.WHITE);

        // Render the current path, if there is one
        double currentLength = 0;
        if (current != null && current.getPath() != null) {
            int renderBegin = Math.max(current.getPosition() - 3, 0);
            List<BetterBlockPos> positions = current.getPath().positions();
            drawPath(event.getModelViewStack(), positions, renderBegin, settings.colorCurrentPath.value, 0, true);
            currentLength = PathRibbon.length(positions, 0, positions.size() - 1, true);
        }

        if (next != null && next.getPath() != null) {
            // next starts where current ends, so it picks up the shimmer right where current drops it
            drawPath(event.getModelViewStack(), next.getPath().positions(), 0, settings.colorNextPath.value, currentLength, true);
        }

        // If there is a path calculation currently running, render the path calculation process
        // the classic one flickers like crazy, which is how you know it's thinking. some people like that
        if (settings.renderPathRibbon.value && settings.renderSearchSmooth.value) {
            // every frame, search or no search, because it has things to fade out after the search is over
            SEARCH_GLOW.computeIfAbsent(behavior, b -> new SearchGlow()).render(event.getModelViewStack(), behavior.getInProgress());
        } else {
            behavior.getInProgress().ifPresent(currentlyRunning -> drawSearchClassic(event.getModelViewStack(), ctx, currentlyRunning));
        }
    }

    private static void drawSearchClassic(PoseStack stack, IPlayerContext ctx, IPathFinder currentlyRunning) {
        currentlyRunning.bestPathSoFar().ifPresent(p -> {
            drawPath(stack, p.positions(), 0, settings.colorBestPathSoFar.value, 0, false);
        });

        currentlyRunning.pathToMostRecentNodeConsidered().ifPresent(mr -> {
            drawPath(stack, mr.positions(), 0, settings.colorMostRecentConsidered.value, 0, false);
            drawManySelectionBoxes(stack, ctx.player(), Collections.singletonList(mr.getDest()), settings.colorMostRecentConsidered.value);
        });
    }

    public static void drawPath(PoseStack stack, List<BetterBlockPos> positions, int startIndex, Color color, boolean fadeOut, int fadeStart0, int fadeEnd0) {
        drawPath(stack, positions, startIndex, color, fadeOut, fadeStart0, fadeEnd0, 0.5D);
    }

    public static void drawPath(PoseStack stack, List<BetterBlockPos> positions, int startIndex, Color color, boolean fadeOut, int fadeStart0, int fadeEnd0, double offset) {
        if (settings.renderPathRibbon.value) {
            // whoever is calling this from outside might not be drawing something that walks (elytra isn't), so no steps
            PathRibbon.draw(stack, positions, startIndex, color, fadeOut, fadeStart0, fadeEnd0, offset, 0, 1.0F, 1.0F, true, false, true);
        } else {
            drawPathLines(stack, positions, startIndex, color, fadeOut, fadeStart0, fadeEnd0, offset);
        }
    }

    private static void drawPath(PoseStack stack, List<BetterBlockPos> positions, int startIndex, Color color, double arcOffset, boolean animated) {
        if (settings.renderPathRibbon.value) {
            PathRibbon.draw(stack, positions, startIndex, color, settings.fadePath.value, 10, 20, 0.5D, arcOffset, 1.0F, 1.0F, animated, true, true);
        } else {
            drawPathLines(stack, positions, startIndex, color, settings.fadePath.value, 10, 20, 0.5D);
        }
    }

    private static void drawPathLines(PoseStack stack, List<BetterBlockPos> positions, int startIndex, Color color, boolean fadeOut, int fadeStart0, int fadeEnd0, double offset) {
        BufferBuilder bufferBuilder = IRenderer.startLines(color, settings.pathRenderLineWidthPixels.value, settings.renderPathIgnoreDepth.value);

        int fadeStart = fadeStart0 + startIndex;
        int fadeEnd = fadeEnd0 + startIndex;

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

            emitPathLine(bufferBuilder, stack, start.x, start.y, start.z, end.x, end.y, end.z, offset);
        }

        IRenderer.endLines(bufferBuilder, settings.renderPathIgnoreDepth.value);
    }

    private static void emitPathLine(BufferBuilder bufferBuilder, PoseStack stack, double x1, double y1, double z1, double x2, double y2, double z2, double offset) {
        final double extraOffset = offset + 0.03D;

        double vpX = posX();
        double vpY = posY();
        double vpZ = posZ();
        boolean renderPathAsFrickinThingy = !settings.renderPathAsLine.value;

        IRenderer.emitLine(bufferBuilder, stack,
                x1 + offset - vpX, y1 + offset - vpY, z1 + offset - vpZ,
                x2 + offset - vpX, y2 + offset - vpY, z2 + offset - vpZ
        );
        if (renderPathAsFrickinThingy) {
            IRenderer.emitLine(bufferBuilder, stack,
                    x2 + offset - vpX, y2 + offset - vpY, z2 + offset - vpZ,
                    x2 + offset - vpX, y2 + extraOffset - vpY, z2 + offset - vpZ
            );
            IRenderer.emitLine(bufferBuilder, stack,
                    x2 + offset - vpX, y2 + extraOffset - vpY, z2 + offset - vpZ,
                    x1 + offset - vpX, y1 + extraOffset - vpY, z1 + offset - vpZ
            );
            IRenderer.emitLine(bufferBuilder, stack,
                    x1 + offset - vpX, y1 + extraOffset - vpY, z1 + offset - vpZ,
                    x1 + offset - vpX, y1 + offset - vpY, z1 + offset - vpZ
            );
        }
    }

    public static void drawManySelectionBoxes(PoseStack stack, Entity player, Collection<BlockPos> positions, Color color) {
        if (positions.isEmpty()) {
            return;
        }
        boolean ignoreDepth = settings.renderSelectionBoxesIgnoreDepth.value;

        //BlockPos blockpos = movingObjectPositionIn.getBlockPos();
        BlockStateInterface bsi = new BlockStateInterface(BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext()); // TODO this assumes same dimension between primary baritone and render view? is this safe?

        List<AABB> boxes = new ArrayList<>(positions.size());
        positions.forEach(pos -> {
            BlockState state = bsi.get0(pos);
            VoxelShape shape = state.getShape(player.level(), pos);
            AABB toDraw = shape.isEmpty() ? Shapes.block().bounds() : shape.bounds();
            boxes.add(toDraw.move(pos).inflate(.002D));
        });

        // two passes because there's only the one tesselator, quads and lines can't be open at the same time
        if (settings.renderBoxFill.value) {
            BufferBuilder quads = IRenderer.startQuads(ignoreDepth);
            IRenderer.glColor(color, BOX_FILL_ALPHA);
            boxes.forEach(box -> IRenderer.emitFilledAABB(quads, stack, box, BOX_FILL_ALPHA));
            IRenderer.endQuads(quads, ignoreDepth);
        }

        BufferBuilder bufferBuilder = IRenderer.startLines(color, outlineAlpha(), boxLineWidth(), ignoreDepth);
        boxes.forEach(box -> IRenderer.emitAABB(bufferBuilder, stack, box));
        IRenderer.endLines(bufferBuilder, ignoreDepth);
    }

    // fat wireframes are most of why this looked like 2014. once there's a fill to carry the box the outline can calm down
    private static float boxLineWidth() {
        float width = settings.pathRenderLineWidthPixels.value;
        return settings.renderBoxFill.value ? width / 2 : width;
    }

    // without the fill it's the classic look, and the classic look has see through outlines
    private static float outlineAlpha() {
        return settings.renderBoxFill.value ? OUTLINE_ALPHA : 0.4F;
    }

    public static void drawGoal(PoseStack stack, IPlayerContext ctx, Goal goal, float partialTicks, Color color) {
        // figure out every box first and draw after. composites can nest and mix goal types,
        // and this way they all end up in one batch of fills and one batch of lines no matter what's in there
        List<GoalBox> boxes = new ArrayList<>();
        collectGoalBoxes(boxes, stack, ctx, goal, partialTicks, color);
        if (boxes.isEmpty()) {
            return;
        }
        boolean ignoreDepth = settings.renderGoalIgnoreDepth.value;

        if (settings.renderBoxFill.value) {
            BufferBuilder quads = IRenderer.startQuads(ignoreDepth);
            boxes.forEach(box -> box.fill(quads, stack));
            IRenderer.endQuads(quads, ignoreDepth);
        }

        BufferBuilder bufferBuilder = IRenderer.startLines(color, outlineAlpha(), settings.goalRenderLineWidthPixels.value, ignoreDepth);
        boxes.forEach(box -> box.outline(bufferBuilder, stack));
        IRenderer.endLines(bufferBuilder, ignoreDepth);
    }

    // -1 to 1 and back every two seconds. the rings ride it, and the fill breathes along with it
    private static double goalWave() {
        return Mth.cos((float) (((float) ((System.nanoTime() / 100000L) % 20000L)) / 20000F * Math.PI * 2));
    }

    private static void collectGoalBoxes(List<GoalBox> boxes, PoseStack stack, IPlayerContext ctx, Goal goal, float partialTicks, Color color) {
        double renderPosX = posX();
        double renderPosY = posY();
        double renderPosZ = posZ();
        double minX, maxX;
        double minZ, maxZ;
        double minY, maxY;
        double y, y1, y2;
        float breath = 1;
        if (!settings.renderGoalAnimated.value) {
            // y = 1 causes rendering issues when the player is at the same y as the top of a block for some reason
            y = 0.999F;
        } else {
            y = goalWave();
            breath = 0.75F + 0.25F * (float) y;
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
            // bright at the floor and gone by the top, like the block is glowing upwards
            boxes.add(new GoalBox(color, minX, maxX, minZ, maxZ, minY, maxY, y1, y2, GOAL_FILL_ALPHA * breath, 0));
        } else if (goal instanceof GoalXZ) {
            GoalXZ goalPos = (GoalXZ) goal;
            minY = ctx.world().getMinY();
            maxY = ctx.world().getMaxY();

            if (settings.renderGoalXZBeacon.value) {
                //TODO: check
                textureManager.getTexture(TEXTURE_BEACON_BEAM).bind();
                if (settings.renderGoalIgnoreDepth.value) {
                    RenderSystem.disableDepthTest();
                }

                stack.pushPose(); // push
                stack.translate(goalPos.getX() - renderPosX, -renderPosY, goalPos.getZ() - renderPosZ); // translate

                //TODO: check
                BeaconRenderer.renderBeaconBeam(
                        stack,
                        ctx.minecraft().renderBuffers().bufferSource(),
                        TEXTURE_BEACON_BEAM,
                        settings.renderGoalAnimated.value ? partialTicks : 0,
                        1.0F,
                        settings.renderGoalAnimated.value ? ctx.world().getGameTime() : 0,
                        (int) minY,
                        (int) maxY,
                        color.getRGB(),

                        // Arguments filled by the private method lol
                        0.2F,
                        0.25F
                );

                stack.popPose(); // pop

                if (settings.renderGoalIgnoreDepth.value) {
                    RenderSystem.enableDepthTest();
                }
                return;
            }

            minX = goalPos.getX() + 0.002 - renderPosX;
            maxX = goalPos.getX() + 1 - 0.002 - renderPosX;
            minZ = goalPos.getZ() + 0.002 - renderPosZ;
            maxZ = goalPos.getZ() + 1 - 0.002 - renderPosZ;

            y1 = 0;
            y2 = 0;
            minY -= renderPosY;
            maxY -= renderPosY;
            // a gradient over 384 blocks is just a flat color with extra steps
            float alpha = GOAL_FILL_ALPHA / 2 * breath;
            boxes.add(new GoalBox(color, minX, maxX, minZ, maxZ, minY, maxY, y1, y2, alpha, alpha));
        } else if (goal instanceof GoalComposite) {
            for (Goal g : ((GoalComposite) goal).goals()) {
                collectGoalBoxes(boxes, stack, ctx, g, partialTicks, color);
            }
        } else if (goal instanceof GoalInverted) {
            collectGoalBoxes(boxes, stack, ctx, ((GoalInverted) goal).origin, partialTicks, settings.colorInvertedGoalBox.value);
        } else if (goal instanceof GoalYLevel) {
            GoalYLevel goalpos = (GoalYLevel) goal;
            minX = ctx.player().position().x - settings.yLevelBoxSize.value - renderPosX;
            minZ = ctx.player().position().z - settings.yLevelBoxSize.value - renderPosZ;
            maxX = ctx.player().position().x + settings.yLevelBoxSize.value - renderPosX;
            maxZ = ctx.player().position().z + settings.yLevelBoxSize.value - renderPosZ;
            minY = ((GoalYLevel) goal).level - renderPosY;
            maxY = minY + 2;
            y1 = 1 + y + goalpos.level - renderPosY;
            y2 = 1 - y + goalpos.level - renderPosY;
            // this one is thirty blocks wide and you're usually standing in it, so go easy
            boxes.add(new GoalBox(color, minX, maxX, minZ, maxZ, minY, maxY, y1, y2, GOAL_FILL_ALPHA / 3 * breath, 0));
        }
    }

    private static void renderHorizontalQuad(BufferBuilder bufferBuilder, PoseStack stack, double minX, double maxX, double minZ, double maxZ, double y) {
        if (y != 0) {
            IRenderer.emitLine(bufferBuilder, stack, minX, y, minZ, maxX, y, minZ, 1.0, 0.0, 0.0);
            IRenderer.emitLine(bufferBuilder, stack, maxX, y, minZ, maxX, y, maxZ, 0.0, 0.0, 1.0);
            IRenderer.emitLine(bufferBuilder, stack, maxX, y, maxZ, minX, y, maxZ, -1.0, 0.0, 0.0);
            IRenderer.emitLine(bufferBuilder, stack, minX, y, maxZ, minX, y, minZ, 0.0, 0.0, -1.0);
        }
    }

    // one dank lit goal box, camera relative, remembered for long enough to draw it twice
    // y1 and y2 are the two rings, zero means no ring
    private record GoalBox(Color color, double minX, double maxX, double minZ, double maxZ, double minY, double maxY, double y1, double y2, float bottomAlpha, float topAlpha) {

        private void fill(BufferBuilder quads, PoseStack stack) {
            IRenderer.glColor(color, bottomAlpha);
            IRenderer.emitWalls(quads, stack, minX, minY, minZ, maxX, maxY, maxZ, bottomAlpha, topAlpha);
            if (y1 == 0 && y2 == 0) {
                // the xz column. its floor is at bedrock, nobody is going to miss it
                return;
            }
            IRenderer.emitHorizontalQuad(quads, stack, minX, minZ, maxX, maxZ, minY, bottomAlpha);
            // the rings get a faint pane each, so they look like they're scanning the box and not just floating in it
            IRenderer.emitHorizontalQuad(quads, stack, minX, minZ, maxX, maxZ, y1, bottomAlpha / 2);
            IRenderer.emitHorizontalQuad(quads, stack, minX, minZ, maxX, maxZ, y2, bottomAlpha / 2);
        }

        private void outline(BufferBuilder bufferBuilder, PoseStack stack) {
            IRenderer.glColor(color, outlineAlpha());
            renderHorizontalQuad(bufferBuilder, stack, minX, maxX, minZ, maxZ, y1);
            renderHorizontalQuad(bufferBuilder, stack, minX, maxX, minZ, maxZ, y2);

            for (double y = minY; y < maxY; y += 16) {
                double max = Math.min(maxY, y + 16);
                IRenderer.emitLine(bufferBuilder, stack, minX, y, minZ, minX, max, minZ, 0.0, 1.0, 0.0);
                IRenderer.emitLine(bufferBuilder, stack, maxX, y, minZ, maxX, max, minZ, 0.0, 1.0, 0.0);
                IRenderer.emitLine(bufferBuilder, stack, maxX, y, maxZ, maxX, max, maxZ, 0.0, 1.0, 0.0);
                IRenderer.emitLine(bufferBuilder, stack, minX, y, maxZ, minX, max, maxZ, 0.0, 1.0, 0.0);
            }
        }
    }
}
