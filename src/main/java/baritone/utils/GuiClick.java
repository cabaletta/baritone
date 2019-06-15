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
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalTwoBlocks;
import baritone.api.utils.BetterBlockPos;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.glu.GLU;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collections;

import static org.lwjgl.opengl.GL11.*;

public class GuiClick extends GuiScreen {

    // My name is Brady and I grant leijurv permission to use this pasted code
    private final FloatBuffer MODELVIEW = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer PROJECTION = BufferUtils.createFloatBuffer(16);
    private final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
    private final FloatBuffer TO_WORLD_BUFFER = BufferUtils.createFloatBuffer(3);

    private BlockPos clickStart;
    private BlockPos currentMouseOver;

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        int mx = Mouse.getX();
        int my = Mouse.getY();
        Vec3d near = toWorld(mx, my, 0);
        Vec3d far = toWorld(mx, my, 1); // "Use 0.945 that's what stack overflow says" - leijurv
        if (near != null && far != null) {
            Vec3d viewerPos = new Vec3d(mc.getRenderManager().viewerPosX, mc.getRenderManager().viewerPosY, mc.getRenderManager().viewerPosZ);
            RayTraceResult result = mc.world.rayTraceBlocks(near.add(viewerPos), far.add(viewerPos), false, false, true);
            if (result != null && result.typeOfHit == RayTraceResult.Type.BLOCK) {
                currentMouseOver = result.getBlockPos();
            }
        }

    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            if (clickStart != null && !clickStart.equals(currentMouseOver)) {
                ((Baritone) BaritoneAPI.getProvider().getPrimaryBaritone()).getBuilderProcess().clearArea(clickStart, currentMouseOver);
                clickStart = null;
            } else {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalTwoBlocks(currentMouseOver));
            }
        } else if (mouseButton == 1) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(currentMouseOver.up()));
        }
        clickStart = null;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        clickStart = currentMouseOver;
    }

    public void onRender() {
        GlStateManager.getFloat(GL_MODELVIEW_MATRIX, (FloatBuffer) MODELVIEW.clear());
        GlStateManager.getFloat(GL_PROJECTION_MATRIX, (FloatBuffer) PROJECTION.clear());
        GlStateManager.glGetInteger(GL_VIEWPORT, (IntBuffer) VIEWPORT.clear());

        if (currentMouseOver != null) {
            Entity e = mc.getRenderViewEntity();
            // drawSingleSelectionBox WHEN?
            PathRenderer.drawManySelectionBoxes(e, Collections.singletonList(currentMouseOver), Color.CYAN);
            if (clickStart != null && !clickStart.equals(currentMouseOver)) {
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.color(Color.RED.getColorComponents(null)[0], Color.RED.getColorComponents(null)[1], Color.RED.getColorComponents(null)[2], 0.4F);
                GlStateManager.glLineWidth(Baritone.settings().pathRenderLineWidthPixels.value);
                GlStateManager.disableTexture2D();
                GlStateManager.depthMask(false);
                GlStateManager.disableDepth();
                BetterBlockPos a = new BetterBlockPos(currentMouseOver);
                BetterBlockPos b = new BetterBlockPos(clickStart);
                PathRenderer.drawAABB(new AxisAlignedBB(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z), Math.max(a.x, b.x) + 1, Math.max(a.y, b.y) + 1, Math.max(a.z, b.z) + 1));
                GlStateManager.enableDepth();

                GlStateManager.depthMask(true);
                GlStateManager.enableTexture2D();
                GlStateManager.disableBlend();
            }
        }
    }

    private Vec3d toWorld(double x, double y, double z) {
        boolean result = GLU.gluUnProject((float) x, (float) y, (float) z, MODELVIEW, PROJECTION, VIEWPORT, (FloatBuffer) TO_WORLD_BUFFER.clear());
        if (result) {
            return new Vec3d(TO_WORLD_BUFFER.get(0), TO_WORLD_BUFFER.get(1), TO_WORLD_BUFFER.get(2));
        }
        return null;
    }
}
