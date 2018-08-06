package baritone.bot.behavior.impl;

import baritone.bot.behavior.Behavior;
import baritone.bot.event.events.ChatEvent;
import baritone.bot.event.events.RenderEvent;
import baritone.bot.event.events.TickEvent;
import baritone.bot.pathing.calc.AStarPathFinder;
import baritone.bot.pathing.calc.AbstractNodeCostSearch;
import baritone.bot.pathing.calc.IPathFinder;
import baritone.bot.pathing.goals.Goal;
import baritone.bot.pathing.goals.GoalBlock;
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
        // System.out.println("Ticking");
        if (current == null) {
            return;
        }
        current.onTick(event);
        if (current.failed() || current.finished()) {
            current = null;
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

            try {
                findPath(start).map(PathExecutor::new).ifPresent(path -> current = path);
            } catch (Exception ignored) {}
            /*isThereAnythingInProgress = false;
            if (!currentPath.goal.isInGoal(currentPath.end)) {
                if (talkAboutIt) {
                    Out.gui("I couldn't get all the way to " + goal + ", but I'm going to get as close as I can. " + currentPath.numNodes + " nodes considered", Out.Mode.Standard);
                }
                planAhead();
            } else if (talkAboutIt) {
                Out.gui("Finished finding a path from " + start + " to " + goal + ". " + currentPath.numNodes + " nodes considered", Out.Mode.Debug);
            }*/
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

        // Render the current path, if there is one
        getPath().ifPresent(path -> drawPath(path, player(), partialTicks, Color.RED));

        // If there is a path calculation currently running, render the path calculation process
        AbstractNodeCostSearch.getCurrentlyRunning().ifPresent(currentlyRunning -> {
            currentlyRunning.bestPathSoFar().ifPresent(p -> {
                drawPath(p, player(), partialTicks, Color.BLUE);
                currentlyRunning.pathToMostRecentNodeConsidered().ifPresent(mr -> {
                    drawPath(mr, player(), partialTicks, Color.CYAN);
                    drawSelectionBox(player(), mr.getDest(), partialTicks, Color.CYAN);
                });
            });
        });
    }

    public void drawPath(IPath path, EntityPlayerSP player, float partialTicks, Color color) {

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(color.getColorComponents(null)[0], color.getColorComponents(null)[1], color.getColorComponents(null)[2], 0.4F);
        GL11.glLineWidth(3.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);


        List<BlockPos> positions = path.positions();
        for (int i = 0; i < positions.size() - 1; i++) {
            BlockPos a = positions.get(i);
            BlockPos b = positions.get(i + 1);
            double x1 = a.getX();
            double y1 = a.getY();
            double z1 = a.getZ();
            double x2 = b.getX();
            double y2 = b.getY();
            double z2 = b.getZ();
            drawLine(player, x1, y1, z1, x2, y2, z2, partialTicks);
        }
        //GlStateManager.color(0.0f, 0.0f, 0.0f, 0.4f);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawLine(EntityPlayer player, double bp1x, double bp1y, double bp1z, double bp2x, double bp2y, double bp2z, float partialTicks) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder worldrenderer = tessellator.getBuffer();
        double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTicks;
        double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTicks;
        double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTicks;
        worldrenderer.begin(3, DefaultVertexFormats.POSITION);
        worldrenderer.pos(bp1x + 0.5D - d0, bp1y + 0.5D - d1, bp1z + 0.5D - d2).endVertex();
        worldrenderer.pos(bp2x + 0.5D - d0, bp2y + 0.5D - d1, bp2z + 0.5D - d2).endVertex();
        worldrenderer.pos(bp2x + 0.5D - d0, bp2y + 0.53D - d1, bp2z + 0.5D - d2).endVertex();
        worldrenderer.pos(bp1x + 0.5D - d0, bp1y + 0.53D - d1, bp1z + 0.5D - d2).endVertex();
        worldrenderer.pos(bp1x + 0.5D - d0, bp1y + 0.5D - d1, bp1z + 0.5D - d2).endVertex();
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
        AxisAlignedBB toDraw = block.getSelectedBoundingBox(state, Minecraft.getMinecraft().world, blockpos).expand(0.0020000000949949026D, 0.0020000000949949026D, 0.0020000000949949026D).offset(-d0, -d1, -d2);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder worldrenderer = tessellator.getBuffer();
        worldrenderer.begin(3, DefaultVertexFormats.POSITION);
        worldrenderer.pos(toDraw.minX, toDraw.minY, toDraw.minZ).endVertex();
        worldrenderer.pos(toDraw.maxX, toDraw.minY, toDraw.minZ).endVertex();
        worldrenderer.pos(toDraw.maxX, toDraw.minY, toDraw.maxZ).endVertex();
        worldrenderer.pos(toDraw.minX, toDraw.minY, toDraw.maxZ).endVertex();
        worldrenderer.pos(toDraw.minX, toDraw.minY, toDraw.minZ).endVertex();
        tessellator.draw();
        worldrenderer.begin(3, DefaultVertexFormats.POSITION);
        worldrenderer.pos(toDraw.minX, toDraw.maxY, toDraw.minZ).endVertex();
        worldrenderer.pos(toDraw.maxX, toDraw.maxY, toDraw.minZ).endVertex();
        worldrenderer.pos(toDraw.maxX, toDraw.maxY, toDraw.maxZ).endVertex();
        worldrenderer.pos(toDraw.minX, toDraw.maxY, toDraw.maxZ).endVertex();
        worldrenderer.pos(toDraw.minX, toDraw.maxY, toDraw.minZ).endVertex();
        tessellator.draw();
        worldrenderer.begin(1, DefaultVertexFormats.POSITION);
        worldrenderer.pos(toDraw.minX, toDraw.minY, toDraw.minZ).endVertex();
        worldrenderer.pos(toDraw.minX, toDraw.maxY, toDraw.minZ).endVertex();
        worldrenderer.pos(toDraw.maxX, toDraw.minY, toDraw.minZ).endVertex();
        worldrenderer.pos(toDraw.maxX, toDraw.maxY, toDraw.minZ).endVertex();
        worldrenderer.pos(toDraw.maxX, toDraw.minY, toDraw.maxZ).endVertex();
        worldrenderer.pos(toDraw.maxX, toDraw.maxY, toDraw.maxZ).endVertex();
        worldrenderer.pos(toDraw.minX, toDraw.minY, toDraw.maxZ).endVertex();
        worldrenderer.pos(toDraw.minX, toDraw.maxY, toDraw.maxZ).endVertex();
        tessellator.draw();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
