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
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalTwoBlocks;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceFluidMode;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collections;

import static org.lwjgl.opengl.GL11.*;

public class GuiClickMeme extends GuiScreen {

    // My name is Brady and I grant Leijurv permission to use this pasted code
    private final FloatBuffer MODELVIEW = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer PROJECTION = BufferUtils.createFloatBuffer(16);
    private final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
    private final FloatBuffer TO_WORLD_BUFFER = BufferUtils.createFloatBuffer(3);

    private BlockPos meme;

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        double mx = mc.mouseHelper.getMouseX();
        double my = mc.mouseHelper.getMouseY();
        Vec3d near = toWorld(mx, my, 0);
        Vec3d far = toWorld(mx, my, 1); // "Use 0.945 that's what stack overflow says" - Leijurv
        if (near != null && far != null) {
            Vec3d viewerPos = new Vec3d(mc.getRenderManager().viewerPosX, mc.getRenderManager().viewerPosY, mc.getRenderManager().viewerPosZ);
            RayTraceResult result = mc.world.rayTraceBlocks(near.add(viewerPos), far.add(viewerPos), RayTraceFluidMode.NEVER, false, true);
            if (result != null && result.type == RayTraceResult.Type.BLOCK) {
                meme = result.getBlockPos();
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalTwoBlocks(meme));
        } else if (mouseButton == 1) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(meme.up()));
        }
        return false;
    }

    public void onRender() {
        GlStateManager.getFloatv(GL_MODELVIEW_MATRIX, (FloatBuffer) MODELVIEW.clear());
        GlStateManager.getFloatv(GL_PROJECTION_MATRIX, (FloatBuffer) PROJECTION.clear());
        GL11.glGetIntegerv(GL_VIEWPORT, (IntBuffer) VIEWPORT.clear());

        if (meme != null) {
            Entity e = mc.getRenderViewEntity();
            // drawSingleSelectionBox WHEN?
            PathRenderer.drawManySelectionBoxes(e, Collections.singletonList(meme), Color.CYAN);
        }
    }

    public Vec3d toWorld(double x, double y, double z) {
        // TODO: (1.13.2) Fix unproject
//        boolean result = GLU.gluUnProject((float) x, (float) y, (float) z, MODELVIEW, PROJECTION, VIEWPORT, (FloatBuffer) TO_WORLD_BUFFER.clear());
//        if (result) {
//            return new Vec3d(TO_WORLD_BUFFER.get(0), TO_WORLD_BUFFER.get(1), TO_WORLD_BUFFER.get(2));
//        }
        return null;
    }
}
