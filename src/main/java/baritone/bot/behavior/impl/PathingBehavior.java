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

package baritone.bot.behavior.impl;

import baritone.bot.Baritone;
import baritone.bot.behavior.Behavior;
import baritone.bot.event.events.ChatEvent;
import baritone.bot.event.events.RenderEvent;
import baritone.bot.event.events.TickEvent;
import baritone.bot.pathing.calc.AStarPathFinder;
import baritone.bot.pathing.calc.AbstractNodeCostSearch;
import baritone.bot.pathing.calc.IPathFinder;
import baritone.bot.pathing.goals.Goal;
import baritone.bot.pathing.goals.GoalBlock;
import baritone.bot.pathing.goals.GoalXZ;
import baritone.bot.pathing.movement.Movement;
import baritone.bot.pathing.path.IPath;
import baritone.bot.pathing.path.PathExecutor;
import baritone.bot.utils.BlockStateInterface;
import net.minecraft.block.Block;
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
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;
import java.util.Optional;

public class PathingBehavior extends Behavior {

    public static final PathingBehavior INSTANCE = new PathingBehavior();

    private PathingBehavior() {}

    private PathExecutor current;

    private Goal goal;

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT || current == null) {
            return;
        }
        current.onTick(event);
        if (current.failed() || current.finished()) {
            current = null;
            if (!goal.isInGoal(playerFeet()))
                findPathInNewThread(playerFeet(), true);
        }
    }

    public PathExecutor getExecutor() {
        return current;
    }

    public Optional<IPath> getPath() {
        return Optional.ofNullable(current).map(PathExecutor::getPath);
    }

    @Override
    public void onSendChatMessage(ChatEvent event) {
        String msg = event.getMessage();
        if (msg.equals("goal")) {
            goal = new GoalBlock(playerFeet());
            displayChatMessageRaw("Goal: " + goal);
            event.cancel();
            return;
        }
        if (msg.equals("path")) {
            findPathInNewThread(playerFeet(), true);
            event.cancel();
            return;
        }
        if (msg.toLowerCase().equals("slowpath")) {
            AStarPathFinder.slowPath ^= true;
            event.cancel();
            return;
        }
        if (msg.toLowerCase().equals("cancel")) {
            current = null;
            Baritone.INSTANCE.getInputOverrideHandler().clearAllKeys();
            event.cancel();
            displayChatMessageRaw("ok canceled");
            return;
        }
        if (msg.toLowerCase().startsWith("thisway")) {
            goal = GoalXZ.fromDirection(playerFeetAsVec(), player().rotationYaw, Double.parseDouble(msg.substring(7).trim()));
            displayChatMessageRaw("Goal: " + goal);
            event.cancel();
            return;
        }
    }

    /**
     * In a new thread, pathfind to target blockpos
     *
     * @param start
     * @param talkAboutIt
     */
    public void findPathInNewThread(final BlockPos start, final boolean talkAboutIt) {
        new Thread(() -> {
            if (talkAboutIt) {
                displayChatMessageRaw("Starting to search for path from " + start + " to " + goal);
            }

            findPath(start).map(PathExecutor::new).ifPresent(path -> current = path);
            /*isThereAnythingInProgress = false;
            if (!currentPath.goal.isInGoal(currentPath.end)) {
                if (talkAboutIt) {
                    Out.gui("I couldn't get all the way to " + goal + ", but I'm going to get as close as I can. " + currentPath.numNodes + " nodes considered", Out.Mode.Standard);
                }
                planAhead();
            } else if (talkAboutIt) {
                Out.gui(, Out.Mode.Debug);
            }*/
            if (talkAboutIt && current != null && current.getPath() != null) {
                displayChatMessageRaw("Finished finding a path from " + start + " to " + goal + ". " + current.getPath().getNumNodesConsidered() + " nodes considered");
            }
        }).start();
    }

    /**
     * Actually do the pathing
     *
     * @param start
     * @return
     */
    private Optional<IPath> findPath(BlockPos start) {
        if (goal == null) {
            displayChatMessageRaw("no goal");
            return Optional.empty();
        }
        try {
            IPathFinder pf = new AStarPathFinder(start, goal);
            return pf.calculate();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public void onRenderPass(RenderEvent event) {
        //System.out.println("Render passing");
        //System.out.println(event.getPartialTicks());
        float partialTicks = event.getPartialTicks();
        long start = System.currentTimeMillis();

        // Render the current path, if there is one
        if (current != null && current.getPath() != null) {
            int renderBegin = Math.max(current.getPosition() - 3, 0);
            drawPath(current.getPath(), renderBegin, player(), partialTicks, Color.RED);
        }
        long split = System.currentTimeMillis();
        getPath().ifPresent(path -> {
            for (Movement m : path.movements()) {
                for (BlockPos pos : m.toPlace())
                    drawSelectionBox(player(), pos, partialTicks, Color.GREEN);
                for (BlockPos pos : m.toBreak()) {
                    drawSelectionBox(player(), pos, partialTicks, Color.RED);
                }
            }
        });

        // If there is a path calculation currently running, render the path calculation process
        AbstractNodeCostSearch.getCurrentlyRunning().ifPresent(currentlyRunning -> {
            currentlyRunning.bestPathSoFar().ifPresent(p -> {
                drawPath(p, 0, player(), partialTicks, Color.BLUE);
                currentlyRunning.pathToMostRecentNodeConsidered().ifPresent(mr -> {
                    drawPath(mr, 0, player(), partialTicks, Color.CYAN);
                    drawSelectionBox(player(), mr.getDest(), partialTicks, Color.CYAN);
                });
            });
        });
        long end = System.currentTimeMillis();
        // if (end - start > 0)
        //   System.out.println("Frame took " + (split - start) + " " + (end - split));
    }

    private static BlockPos diff(BlockPos a, BlockPos b) {
        return new BlockPos(a.getX() - b.getX(), a.getY() - b.getY(), a.getZ() - b.getZ());
    }

    public void drawPath(IPath path, int startIndex, EntityPlayerSP player, float partialTicks, Color color) {

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
            BlockPos direction = diff(start, end);
            while (next + 1 < positions.size() && direction.equals(diff(end, positions.get(next + 1)))) {
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
        //GlStateManager.color(0.0f, 0.0f, 0.0f, 0.4f);
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
        buffer.begin(3, DefaultVertexFormats.POSITION);
        buffer.pos(bp1x + 0.5D - d0, bp1y + 0.5D - d1, bp1z + 0.5D - d2).endVertex();
        buffer.pos(bp2x + 0.5D - d0, bp2y + 0.5D - d1, bp2z + 0.5D - d2).endVertex();
        buffer.pos(bp2x + 0.5D - d0, bp2y + 0.53D - d1, bp2z + 0.5D - d2).endVertex();
        buffer.pos(bp1x + 0.5D - d0, bp1y + 0.53D - d1, bp1z + 0.5D - d2).endVertex();
        buffer.pos(bp1x + 0.5D - d0, bp1y + 0.5D - d1, bp1z + 0.5D - d2).endVertex();
        tessellator.draw();
    }

    public static void drawSelectionBox(EntityPlayer player, BlockPos blockpos, float partialTicks, Color color) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(color.getColorComponents(null)[0], color.getColorComponents(null)[1], color.getColorComponents(null)[2], 0.4F);
        GL11.glLineWidth(5.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        float f = 0.002F;
        //BlockPos blockpos = movingObjectPositionIn.getBlockPos();
        IBlockState state = BlockStateInterface.get(blockpos);
        Block block = state.getBlock();
        if (block.equals(Blocks.AIR)) {
            block = Blocks.DIRT;
        }
        //block.setBlockBoundsBasedOnState(Minecraft.getMinecraft().world, blockpos);
        double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTicks;
        double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTicks;
        double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTicks;
        AxisAlignedBB toDraw = state.getSelectedBoundingBox(Minecraft.getMinecraft().world, blockpos).expand(0.0020000000949949026D, 0.0020000000949949026D, 0.0020000000949949026D).offset(-d0, -d1, -d2);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(3, DefaultVertexFormats.POSITION);
        buffer.pos(toDraw.minX, toDraw.minY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.minY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.minY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.minX, toDraw.minY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.minX, toDraw.minY, toDraw.minZ).endVertex();
        tessellator.draw();
        buffer.begin(3, DefaultVertexFormats.POSITION);
        buffer.pos(toDraw.minX, toDraw.maxY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.maxY, toDraw.minZ).endVertex();
        buffer.pos(toDraw.maxX, toDraw.maxY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.minX, toDraw.maxY, toDraw.maxZ).endVertex();
        buffer.pos(toDraw.minX, toDraw.maxY, toDraw.minZ).endVertex();
        tessellator.draw();
        buffer.begin(1, DefaultVertexFormats.POSITION);
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
