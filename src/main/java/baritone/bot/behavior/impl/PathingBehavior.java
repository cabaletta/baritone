package baritone.bot.behavior.impl;

import baritone.bot.behavior.Behavior;
import baritone.bot.event.events.ChatEvent;
import baritone.bot.event.events.RenderEvent;
import baritone.bot.pathing.calc.AStarPathFinder;
import baritone.bot.pathing.calc.IPathFinder;
import baritone.bot.pathing.goals.Goal;
import baritone.bot.pathing.goals.GoalBlock;
import baritone.bot.pathing.path.IPath;
import baritone.bot.pathing.path.PathExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;

public class PathingBehavior extends Behavior {

    public static final PathingBehavior INSTANCE = new PathingBehavior();

    private PathingBehavior() {}

    private PathExecutor current;

    private Goal goal;

    @Override
    public void onTick() {
        // System.out.println("Ticking");
        if (current == null) {
            return;
        }
        // current.onTick();
        if (current.failed() || current.finished()) {
            current = null;
        }
    }

    public PathExecutor getExecutor() {
        return current;
    }

    public IPath getPath() {
        if (current == null) {
            return null;
        }
        return current.getPath();
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
                IPath path = findPath(start);
                if (path != null) {
                    current = new PathExecutor(path);
                }
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
    private IPath findPath(BlockPos start) {
        if (goal == null) {
            displayChatMessageRaw("no goal");
            return null;
        }
        try {
            IPathFinder pf = new AStarPathFinder(start, goal);
            IPath path = pf.calculate();
            return path;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onRenderPass(RenderEvent event) {
        // System.out.println("Render passing");
        // System.out.println(event.getPartialTicks());
        drawPath(player(), event.getPartialTicks(), Color.RED);
    }

    public void drawPath(EntityPlayerSP player, float partialTicks, Color color) {

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(color.getColorComponents(null)[0], color.getColorComponents(null)[1], color.getColorComponents(null)[2], 0.4F);
        GL11.glLineWidth(3.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        IPath path = getPath();
        if (path != null) {
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
        }

        // GlStateManager.color(0.0f, 0.0f, 0.0f, 0.4f);
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
}
