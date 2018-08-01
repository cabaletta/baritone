/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.ui;

import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import baritone.Baritone;
import baritone.pathfinding.Path;
import baritone.pathfinding.PathFinder;
import baritone.pathfinding.actions.Action;
import baritone.util.FakeArrow;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

/**
 *
 * @author leijurv
 */
public class PathRenderer {
    public static void render(EntityPlayer player, float partialTicks) {
        if (Baritone.currentPath != null) {
            drawPath(Baritone.currentPath, player, partialTicks, Color.RED);
            for (BlockPos pos : Baritone.currentPath.toMine()) {
                drawSelectionBox(player, pos, partialTicks, Color.RED);
            }
            for (BlockPos pos : Baritone.currentPath.toPlace()) {
                drawSelectionBox(player, pos, partialTicks, Color.GREEN);
            }
        }
        if (Baritone.nextPath != null) {
            drawPath(Baritone.nextPath, player, partialTicks, Color.GREEN);
            for (BlockPos pos : Baritone.nextPath.toMine()) {
                drawSelectionBox(player, pos, partialTicks, Color.RED);
            }
            for (BlockPos pos : Baritone.nextPath.toPlace()) {
                drawSelectionBox(player, pos, partialTicks, Color.GREEN);
            }
        }
        try {
            if (PathFinder.currentlyRunning != null) {
                Path p = PathFinder.currentlyRunning.getTempSolution();
                if (p != null) {
                    drawPath(p, player, partialTicks, Color.BLUE);
                    Path mr = PathFinder.currentlyRunning.getMostRecentNodeConsidered();
                    if (mr != null) {
                        drawPath(mr, player, partialTicks, Color.CYAN);
                        drawSelectionBox(player, mr.end, partialTicks, Color.CYAN);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(PathRenderer.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (!Baritone.sketchytracer) {
            return;
        }
        try {
            ItemStack holding = Minecraft.getMinecraft().player.inventory.mainInventory[Minecraft.getMinecraft().player.inventory.currentItem];
            if (holding != null && (holding.getItem() instanceof ItemBow)) {
                if (Minecraft.getMinecraft().player.getItemInUse() != null) {
                    int i = holding.getItem().getMaxItemUseDuration(holding) - Minecraft.getMinecraft().player.getItemInUseCount();
                    float f = (float) i / 20.0F;
                    f = (f * f + f * 2.0F) / 3.0F;
                    if ((double) f < 0.1D) {
                        return;
                    }
                    if (f > 1.0F) {
                        f = 1.0F;
                    }
                    drawBowStuff(player, partialTicks, f * 2);
                } else {
                    drawBowStuff(player, partialTicks, 2.0F);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(PathRenderer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public static void drawBowStuff(EntityPlayer player, float partialTicks, float strength) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        FakeArrow ar1 = new FakeArrow(Minecraft.getMinecraft().world, Minecraft.getMinecraft().player, strength, partialTicks);
        for (int i = 0; i < 200; i++) {
            ar1.onUpdate();
        }
        Color color = ar1.didIHitAnEntity ? Color.RED : Color.BLACK;
        GlStateManager.color(color.getColorComponents(null)[0], color.getColorComponents(null)[1], color.getColorComponents(null)[2], 0.4F);
        GL11.glLineWidth(3.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        FakeArrow ar = new FakeArrow(Minecraft.getMinecraft().world, Minecraft.getMinecraft().player, strength, partialTicks);
        double prevX = ar.posX;
        double prevY = ar.posY;
        double prevZ = ar.posZ;
        for (int i = 0; i < 200; i++) {
            ar.onUpdate();
            double currX = ar.posX;
            double currY = ar.posY;
            double currZ = ar.posZ;
            drawLine(Minecraft.getMinecraft().player, prevX - 0.5, prevY - 0.5, prevZ - 0.5, currX - 0.5, currY - 0.5, currZ - 0.5, partialTicks);
            prevX = currX;
            prevY = currY;
            prevZ = currZ;
        }
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
    public static void drawPath(Path path, EntityPlayer player, float partialTicks, Color color) {
        /*for (BlockPos pos : path.path) {
         drawSelectionBox(player, pos, partialTicks, color);
         }*/
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(color.getColorComponents(null)[0], color.getColorComponents(null)[1], color.getColorComponents(null)[2], 0.4F);
        GL11.glLineWidth(3.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        boolean[] cornerFirstHalf = new boolean[path.path.size()];
        boolean[] cornerSecondHalf = new boolean[path.path.size()];
        for (int i = 0; i < path.path.size() - 2; i++) {
            BlockPos a = path.path.get(i);
            BlockPos b = path.path.get(i + 1);
            BlockPos c = path.path.get(i + 2);
            if (a.getY() != b.getY() || b.getY() != c.getY()) {
                continue;
            }
            if (b.getX() - a.getX() == c.getX() - b.getX() && b.getZ() - a.getZ() == c.getZ() - b.getZ()) {
                continue;
            }
            if (a.getX() != b.getX() && a.getZ() != b.getZ()) {
                continue;
            }
            if (b.getX() != c.getX() && b.getZ() != c.getZ()) {
                continue;
            }
            BlockPos corner = new BlockPos(c.getX() - b.getX() + a.getX(), a.getY(), c.getZ() - b.getZ() + a.getZ());
            if (Action.avoidWalkingInto(corner) || Action.avoidWalkingInto(corner.up())) {
                continue;
            }
            cornerFirstHalf[i] = true;
            cornerSecondHalf[i + 1] = true;
            double bp1x = (a.getX() + b.getX()) * 0.5D;
            double bp1z = (a.getZ() + b.getZ()) * 0.5D;
            double bp2x = (c.getX() + b.getX()) * 0.5D;
            double bp2z = (c.getZ() + b.getZ()) * 0.5D;
            drawLine(player, bp1x, a.getY(), bp1z, bp2x, a.getY(), bp2z, partialTicks);
        }
        for (int i = 0; i < path.path.size() - 1; i++) {
            BlockPos a = path.path.get(i);
            BlockPos b = path.path.get(i + 1);
            if (cornerFirstHalf[i] && !cornerSecondHalf[i]) {
                drawLine(player, a.getX(), a.getY(), a.getZ(), (b.getX() + a.getX()) * 0.5D, b.getY(), (b.getZ() + a.getZ()) * 0.5D, partialTicks);
            }
            if (cornerSecondHalf[i] && !cornerFirstHalf[i]) {
                drawLine(player, (a.getX() + b.getX()) * 0.5D, a.getY(), (a.getZ() + b.getZ()) * 0.5D, b.getX(), b.getY(), b.getZ(), partialTicks);
            }
            if (!cornerFirstHalf[i] && !cornerSecondHalf[i]) {
                drawLine(player, a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ(), partialTicks);
            } else {
                GlStateManager.color(color.getColorComponents(null)[0], color.getColorComponents(null)[1], color.getColorComponents(null)[2], 0.1F);
                drawLine(player, a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ(), partialTicks);
                GlStateManager.color(color.getColorComponents(null)[0], color.getColorComponents(null)[1], color.getColorComponents(null)[2], 0.4F);
            }
        }
        //GlStateManager.color(0.0f, 0.0f, 0.0f, 0.4f);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
    public static void drawLine(EntityPlayer player, double bp1x, double bp1y, double bp1z, double bp2x, double bp2y, double bp2z, float partialTicks) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
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
        Block block = Baritone.get(blockpos).getBlock();
        if (block.equals(Blocks.air)) {
            block = Blocks.dirt;
        }
        block.setBlockBoundsBasedOnState(Minecraft.getMinecraft().world, blockpos);
        double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTicks;
        double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTicks;
        double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTicks;
        AxisAlignedBB toDraw = block.getSelectedBoundingBox(Minecraft.getMinecraft().world, blockpos).expand(0.0020000000949949026D, 0.0020000000949949026D, 0.0020000000949949026D).offset(-d0, -d1, -d2);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
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
